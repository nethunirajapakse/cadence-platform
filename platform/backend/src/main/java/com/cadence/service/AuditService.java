package com.cadence.service;

import com.cadence.entity.AuditLog;
import com.cadence.entity.enums.AuditAction;
import com.cadence.repository.user.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    // REQUIRES_NEW so the entry is kept even when the surrounding operation
    // fails and rolls back - a failed login or a rejected admin change is
    // exactly what an audit trail should still show.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorUserId, String actorEmail, AuditAction action,
                       String target, String details, String ipAddress) {
        auditLogRepository.save(AuditLog.builder()
                .actorUserId(actorUserId)
                .actorEmail(actorEmail)
                .action(action)
                .target(truncate(target, 255))
                .details(details)
                .ipAddress(truncate(ipAddress, 64))
                .build());
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
