package com.cadence.repository;

import com.cadence.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TimeLogRepository extends JpaRepository<TimeLog, UUID> {
    List<TimeLog> findByReport_ReportId(UUID reportId);
}
