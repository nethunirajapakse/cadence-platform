package com.cadence.service;

import com.cadence.dto.admin.AdminUserResponseDTO;
import com.cadence.dto.admin.AdminUserUpdateRequestDTO;
import com.cadence.entity.Role;
import com.cadence.entity.User;
import com.cadence.entity.enums.AuditAction;
import com.cadence.entity.enums.RoleName;
import com.cadence.exception.ApiException;
import com.cadence.repository.user.RoleRepository;
import com.cadence.repository.user.UserRepository;
import com.cadence.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AdminUserResponseDTO> listUsers() {
        List<User> users = userRepository.findAllWithRoleAndManager();

        Map<UUID, Integer> reportCounts = new HashMap<>();
        for (User user : users) {
            if (user.getManager() != null) {
                reportCounts.merge(user.getManager().getUserId(), 1, Integer::sum);
            }
        }

        return users.stream()
                .map(user -> new AdminUserResponseDTO(user, reportCounts.getOrDefault(user.getUserId(), 0)))
                .toList();
    }

    @Transactional
    public AdminUserResponseDTO update(UserPrincipal admin, UUID userId, AdminUserUpdateRequestDTO request, String ip) {
        User user = load(userId);
        RoleName currentRole = user.getRole().getRoleName();
        RoleName newRole = request.getRole() != null ? request.getRole() : currentRole;
        boolean isSelf = user.getUserId().equals(admin.getUserId());

        StringBuilder changes = new StringBuilder();

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Name cannot be empty");
            }
            if (!name.equals(user.getName())) {
                changes.append("name: ").append(user.getName()).append(" -> ").append(name).append("; ");
                user.setName(name);
            }
        }

        if (newRole != currentRole) {
            if (isSelf) {
                throw new ApiException(HttpStatus.CONFLICT, "You can't change your own role");
            }
            if (currentRole == RoleName.MANAGER && userRepository.countActiveDirectReports(userId) > 0) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "This manager still has team members. Reassign them to another manager first.");
            }
            if (currentRole == RoleName.ADMIN && user.isActive()
                    && userRepository.countActiveByRoleName(RoleName.ADMIN) <= 1) {
                throw new ApiException(HttpStatus.CONFLICT, "At least one active administrator is required");
            }
            Role role = roleRepository.findByRoleName(newRole)
                    .orElseThrow(() -> new IllegalStateException("Role not seeded: " + newRole));
            changes.append("role: ").append(currentRole).append(" -> ").append(newRole).append("; ");
            user.setRole(role);
        }

        User oldManager = user.getManager();
        if (newRole == RoleName.TEAM_MEMBER) {
            UUID managerId = request.getManagerId() != null
                    ? request.getManagerId()
                    : (oldManager == null ? null : oldManager.getUserId());
            if (managerId == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A team member must have a manager");
            }
            if (!Objects.equals(managerId, oldManager == null ? null : oldManager.getUserId())) {
                User manager = userRepository.findByIdWithRole(managerId)
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "The selected manager does not exist"));
                if (manager.getRole().getRoleName() != RoleName.MANAGER || !manager.isActive()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "The selected manager must be an active manager");
                }
                changes.append("manager: ").append(oldManager == null ? "none" : oldManager.getEmail())
                        .append(" -> ").append(manager.getEmail()).append("; ");
                user.setManager(manager);
            }
        } else if (oldManager != null) {
            changes.append("manager: ").append(oldManager.getEmail()).append(" -> none; ");
            user.setManager(null);
        }

        if (changes.length() > 0) {
            userRepository.save(user);
            auditService.record(admin.getUserId(), admin.getEmail(), AuditAction.USER_UPDATED,
                    user.getEmail(), changes.toString().trim(), ip);
        }

        return toDto(user);
    }

    @Transactional
    public AdminUserResponseDTO deactivate(UserPrincipal admin, UUID userId, String ip) {
        User user = load(userId);

        if (user.getUserId().equals(admin.getUserId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You can't deactivate your own account");
        }
        if (!user.isActive()) {
            return toDto(user);
        }

        RoleName role = user.getRole().getRoleName();
        if (role == RoleName.MANAGER && userRepository.countActiveDirectReports(userId) > 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This manager still has team members. Reassign them to another manager first.");
        }
        if (role == RoleName.ADMIN && userRepository.countActiveByRoleName(RoleName.ADMIN) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "At least one active administrator is required");
        }

        // Takes effect on the user's very next request: JwtAuthFilter checks the flag every time.
        user.setActive(false);
        userRepository.save(user);

        auditService.record(admin.getUserId(), admin.getEmail(), AuditAction.USER_DEACTIVATED,
                user.getEmail(), null, ip);
        return toDto(user);
    }

    @Transactional
    public AdminUserResponseDTO reactivate(UserPrincipal admin, UUID userId, String ip) {
        User user = load(userId);

        if (user.isActive()) {
            return toDto(user);
        }
        // A team member whose manager has since been deactivated would come back
        // with nobody able to see their reports - make the admin fix that first.
        User manager = user.getManager();
        if (user.getRole().getRoleName() == RoleName.TEAM_MEMBER && (manager == null || !manager.isActive())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Assign an active manager to this team member before reactivating them.");
        }

        user.setActive(true);
        userRepository.save(user);

        auditService.record(admin.getUserId(), admin.getEmail(), AuditAction.USER_REACTIVATED,
                user.getEmail(), null, ip);
        return toDto(user);
    }

    private User load(UUID userId) {
        return userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private AdminUserResponseDTO toDto(User user) {
        int reports = (int) userRepository.countActiveDirectReports(user.getUserId());
        return new AdminUserResponseDTO(user, reports);
    }
}
