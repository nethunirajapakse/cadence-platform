package com.cadence.repository.user;

import com.cadence.entity.AuditLog;
import com.cadence.entity.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepositoryCustom {
    Page<AuditLog> search(AuditAction action, String actorEmailContains, Pageable pageable);
}
