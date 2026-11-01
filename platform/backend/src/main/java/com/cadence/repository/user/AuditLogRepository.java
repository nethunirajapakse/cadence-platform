package com.cadence.repository.user;

import com.cadence.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, AuditLogRepositoryCustom {
}
