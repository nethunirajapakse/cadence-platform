package com.cadence.repository.report.item;

import com.cadence.entity.NextWeekTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NextWeekTaskRepository extends JpaRepository<NextWeekTask, UUID> {
    List<NextWeekTask> findByReport_ReportId(UUID reportId);
}
