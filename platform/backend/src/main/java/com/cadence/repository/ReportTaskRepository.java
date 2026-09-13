package com.cadence.repository;

import com.cadence.entity.ReportTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportTaskRepository extends JpaRepository<ReportTask, UUID> {
    List<ReportTask> findByReport_ReportId(UUID reportId);
}
