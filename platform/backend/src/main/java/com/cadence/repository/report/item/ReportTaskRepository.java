package com.cadence.repository.report.item;

import com.cadence.entity.ReportTask;
import com.cadence.entity.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReportTaskRepository extends JpaRepository<ReportTask, UUID> {
    List<ReportTask> findByReport_ReportId(UUID reportId);

    // Backs the "tasks completed trend" and "workload by project" charts -
    // fetches the owning report + its project in one query rather than
    // lazy-loading them per task.
    @Query("SELECT t FROM ReportTask t JOIN FETCH t.report r JOIN FETCH r.project WHERE r.status <> :excludedStatus")
    List<ReportTask> findAllExcludingReportStatus(@Param("excludedStatus") ReportStatus excludedStatus);
}
