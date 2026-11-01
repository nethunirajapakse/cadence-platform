package com.cadence.dto.admin;

import com.cadence.entity.AuditLog;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class AuditLogResponseDTO {
    private final UUID auditId;
    private final String actorEmail;
    private final String action;
    private final String target;
    private final String details;
    private final String ipAddress;
    private final LocalDateTime createdAt;

    public AuditLogResponseDTO(AuditLog log) {
        this.auditId = log.getAuditId();
        this.actorEmail = log.getActorEmail();
        this.action = log.getAction().name();
        this.target = log.getTarget();
        this.details = log.getDetails();
        this.ipAddress = log.getIpAddress();
        this.createdAt = log.getCreatedAt();
    }
}
