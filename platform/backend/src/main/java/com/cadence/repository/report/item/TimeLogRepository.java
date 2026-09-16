package com.cadence.repository.report.item;

import com.cadence.entity.TimeLog;
import com.cadence.entity.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TimeLogRepository extends JpaRepository<TimeLog, UUID> {
    List<TimeLog> findByReport_ReportId(UUID reportId);

    // Backs "time spent by task type, team-wide" - excludes DRAFT reports
    // (not real, finalized work yet) but includes SUBMITTED/NEEDS_CORRECTION/
    // APPROVED, since hours logged there all represent real work done.
    @Query("SELECT t FROM TimeLog t JOIN FETCH t.report r WHERE r.status <> :excludedStatus")
    List<TimeLog> findAllExcludingReportStatus(@Param("excludedStatus") ReportStatus excludedStatus);
}
