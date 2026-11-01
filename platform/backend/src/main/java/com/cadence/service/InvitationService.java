package com.cadence.service;

import com.cadence.dto.admin.InvitationRequestDTO;
import com.cadence.dto.admin.InvitationResponseDTO;
import com.cadence.dto.auth.AcceptInviteRequestDTO;
import com.cadence.dto.auth.AuthResponseDTO;
import com.cadence.dto.auth.InvitationPreviewDTO;
import com.cadence.entity.Invitation;
import com.cadence.entity.Role;
import com.cadence.entity.User;
import com.cadence.entity.enums.AuditAction;
import com.cadence.entity.enums.InvitationStatus;
import com.cadence.entity.enums.RoleName;
import com.cadence.exception.ApiException;
import com.cadence.repository.user.InvitationRepository;
import com.cadence.repository.user.RoleRepository;
import com.cadence.repository.user.UserRepository;
import com.cadence.security.JwtService;
import com.cadence.security.UserPrincipal;
import com.cadence.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final String INVALID_LINK_MESSAGE =
            "This invitation link is invalid or has expired. Ask your administrator for a new one.";

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtService jwtService;
    private final MailService mailService;
    private final AuditService auditService;

    @Value("${app.security.invite-ttl-hours:48}")
    private long inviteTtlHours;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // ---- admin side ----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<InvitationResponseDTO> listAll() {
        return invitationRepository.findAllWithDetails().stream().map(InvitationResponseDTO::new).toList();
    }

    @Transactional
    public InvitationResponseDTO create(UserPrincipal admin, InvitationRequestDTO request, String ip) {
        String email = TokenUtil.normalizeEmail(request.getEmail());
        RoleName roleName = request.getRole();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "A user with this email already exists");
        }

        User manager = resolveManagerFor(roleName, request.getManagerId());
        User invitedBy = userRepository.findByIdWithRole(admin.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Inviting user no longer exists"));
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleName));

        // Only one live invitation per address: a new one replaces any pending one.
        for (Invitation existing : invitationRepository.findByEmailAndStatus(email, InvitationStatus.PENDING)) {
            existing.setStatus(InvitationStatus.REVOKED);
        }
        invitationRepository.flush();

        String token = TokenUtil.newToken();
        Invitation invitation = invitationRepository.save(Invitation.builder()
                .email(email)
                .role(role)
                .manager(manager)
                .invitedBy(invitedBy)
                .tokenHash(TokenUtil.hash(token))
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(inviteTtlHours))
                .build());

        mailService.sendInvitation(email, invitedBy.getName(), roleLabel(roleName), linkFor(token), inviteTtlHours);

        auditService.record(admin.getUserId(), admin.getEmail(), AuditAction.INVITE_CREATED,
                email, "role=" + roleName + (manager != null ? ", manager=" + manager.getEmail() : ""), ip);

        return new InvitationResponseDTO(invitation);
    }

    @Transactional
    public InvitationResponseDTO resend(UserPrincipal admin, UUID invitationId, String ip) {
        Invitation invitation = invitationRepository.findByIdWithDetails(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found: " + invitationId));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Only pending invitations can be resent (this one is " + invitation.getStatus() + ")");
        }
        if (userRepository.existsByEmail(invitation.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "A user with this email already exists");
        }

        // A fresh token and expiry each time; the old link stops working.
        String token = TokenUtil.newToken();
        invitation.setTokenHash(TokenUtil.hash(token));
        invitation.setExpiresAt(LocalDateTime.now().plusHours(inviteTtlHours));
        invitationRepository.save(invitation);

        mailService.sendInvitation(invitation.getEmail(), invitation.getInvitedBy().getName(),
                roleLabel(invitation.getRole().getRoleName()), linkFor(token), inviteTtlHours);

        auditService.record(admin.getUserId(), admin.getEmail(), AuditAction.INVITE_RESENT,
                invitation.getEmail(), null, ip);

        return new InvitationResponseDTO(invitation);
    }

    @Transactional
    public void revoke(UserPrincipal admin, UUID invitationId, String ip) {
        Invitation invitation = invitationRepository.findByIdWithDetails(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found: " + invitationId));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Only pending invitations can be revoked (this one is " + invitation.getStatus() + ")");
        }

        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);

        auditService.record(admin.getUserId(), admin.getEmail(), AuditAction.INVITE_REVOKED,
                invitation.getEmail(), null, ip);
    }

    // ---- invitee side ----------------------------------------------------------

    @Transactional(readOnly = true)
    public InvitationPreviewDTO preview(String token) {
        Invitation invitation = findUsableInvitation(token);
        return new InvitationPreviewDTO(
                invitation.getEmail(),
                invitation.getRole().getRoleName().name(),
                invitation.getManager() == null ? null : invitation.getManager().getName(),
                invitation.getExpiresAt());
    }

    @Transactional
    public AuthResult accept(AcceptInviteRequestDTO request, String ip) {
        Invitation invitation = findUsableInvitation(request.getToken());
        String name = request.getName().trim();

        if (name.isEmpty()) {
            throw new com.cadence.exception.FieldValidationException("name", "Name is required");
        }
        passwordPolicy.validate(request.getPassword(), invitation.getEmail(), name);

        if (userRepository.existsByEmail(invitation.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        RoleName roleName = invitation.getRole().getRoleName();
        User manager = invitation.getManager();
        if (roleName == RoleName.TEAM_MEMBER
                && (manager == null || !manager.isActive() || manager.getRole().getRoleName() != RoleName.MANAGER)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "The manager for this invitation is no longer available. Ask your administrator for a new invitation.");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = userRepository.save(User.builder()
                .name(name)
                .email(invitation.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(invitation.getRole())
                .manager(roleName == RoleName.TEAM_MEMBER ? manager : null)
                .passwordChangedAt(now)
                .build());

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(now);
        invitationRepository.save(invitation);

        auditService.record(user.getUserId(), user.getEmail(), AuditAction.INVITE_ACCEPTED,
                user.getEmail(), "role=" + roleName, ip);

        UserPrincipal principal = new UserPrincipal(user);
        String jwt = jwtService.generateToken(principal);
        return new AuthResult(jwt, new AuthResponseDTO(user.getUserId(), user.getName(), user.getEmail(), roleName.name()));
    }

    // ---- helpers -----------------------------------------------------------------

    private Invitation findUsableInvitation(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.GONE, INVALID_LINK_MESSAGE);
        }
        Invitation invitation = invitationRepository.findByTokenHash(TokenUtil.hash(token.trim()))
                .orElseThrow(() -> new ApiException(HttpStatus.GONE, INVALID_LINK_MESSAGE));
        if (!invitation.isUsable(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.GONE, INVALID_LINK_MESSAGE);
        }
        return invitation;
    }

    private User resolveManagerFor(RoleName roleName, UUID managerId) {
        if (roleName != RoleName.TEAM_MEMBER) {
            return null;
        }
        if (managerId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A manager is required when inviting a team member");
        }
        User manager = userRepository.findByIdWithRole(managerId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "The selected manager does not exist"));
        if (manager.getRole().getRoleName() != RoleName.MANAGER || !manager.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The selected manager must be an active manager");
        }
        return manager;
    }

    private String linkFor(String token) {
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return base + "/accept-invite?token=" + token;
    }

    private String roleLabel(RoleName roleName) {
        return switch (roleName) {
            case ADMIN -> "an administrator";
            case MANAGER -> "a manager";
            case TEAM_MEMBER -> "a team member";
        };
    }
}
