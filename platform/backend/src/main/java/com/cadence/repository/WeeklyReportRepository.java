package com.cadence.repository;

import com.cadence.entity.WeeklyReport;
import com.cadence.entity.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, UUID>, WeeklyReportRepositoryCustom {

    Page<WeeklyReport> findByUser_UserId(UUID userId, Pageable pageable);

    // Every report for a given week, any user, any status - the dashboard
    // summary filters DRAFT out itself since it needs to reason about "no
    // report yet" vs "drafted but not submitted" separately.
    List<WeeklyReport> findByWeekStartDate(LocalDate weekStartDate);

    long countByStatus(ReportStatus status);

    // Backs several dashboard charts (status-by-member, recent activity) -
    // JOIN FETCH avoids an N+1 query per report when reading user/project names.
    @Query("SELECT r FROM WeeklyReport r JOIN FETCH r.user JOIN FETCH r.project WHERE r.status <> :excludedStatus")
    List<WeeklyReport> findAllExcludingStatus(@Param("excludedStatus") ReportStatus excludedStatus);
}
