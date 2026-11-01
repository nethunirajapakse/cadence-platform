package com.cadence.dto.admin;

import com.cadence.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class AdminUserResponseDTO {
    private final UUID userId;
    private final String name;
    private final String email;
    private final String role;
    private final UUID managerId;
    private final String managerName;
    private final boolean active;
    private final int directReports;
    private final LocalDateTime createdAt;

    public AdminUserResponseDTO(User user, int directReports) {
        this.userId = user.getUserId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole().getRoleName().name();
        this.managerId = user.getManager() == null ? null : user.getManager().getUserId();
        this.managerName = user.getManager() == null ? null : user.getManager().getName();
        this.active = user.isActive();
        this.directReports = directReports;
        this.createdAt = user.getCreatedAt();
    }
}
