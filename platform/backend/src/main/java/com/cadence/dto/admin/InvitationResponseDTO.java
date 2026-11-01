package com.cadence.dto.admin;

import com.cadence.entity.Invitation;
import com.cadence.entity.enums.InvitationStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class InvitationResponseDTO {
    private final UUID invitationId;
    private final String email;
    private final String role;
    private final UUID managerId;
    private final String managerName;
    private final String invitedByName;
    // PENDING, ACCEPTED, REVOKED, or EXPIRED (a PENDING invitation past its expiry).
    private final String status;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime acceptedAt;

    public InvitationResponseDTO(Invitation invitation) {
        this.invitationId = invitation.getInvitationId();
        this.email = invitation.getEmail();
        this.role = invitation.getRole().getRoleName().name();
        this.managerId = invitation.getManager() == null ? null : invitation.getManager().getUserId();
        this.managerName = invitation.getManager() == null ? null : invitation.getManager().getName();
        this.invitedByName = invitation.getInvitedBy().getName();
        boolean expired = invitation.getStatus() == InvitationStatus.PENDING
                && !invitation.getExpiresAt().isAfter(LocalDateTime.now());
        this.status = expired ? "EXPIRED" : invitation.getStatus().name();
        this.expiresAt = invitation.getExpiresAt();
        this.createdAt = invitation.getCreatedAt();
        this.acceptedAt = invitation.getAcceptedAt();
    }
}
