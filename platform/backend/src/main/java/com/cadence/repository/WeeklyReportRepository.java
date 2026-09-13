package com.cadence.repository;

import com.cadence.entity.WeeklyReport;
import com.cadence.entity.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, UUID> {

    // Team member's own history - paginated, per the "pagination and/or filtering
    // on any endpoint returning a list of reports" requirement.
    Page<WeeklyReport> findByUser_UserId(UUID userId, Pageable pageable);

    // Manager dashboard query - flat visibility across the whole team (no manager-scoped
    // filtering - every manager sees every report). All filters are optional/null-safe
    // so one endpoint covers every filter combination the dashboard needs.
    @Query("""
        SELECT r FROM WeeklyReport r
        WHERE (:userId IS NULL OR r.user.userId = :userId)
          AND (:projectId IS NULL OR r.project.projectId = :projectId)
          AND (:status IS NULL OR r.status = :status)
          AND (:weekStart IS NULL OR r.weekStartDate >= :weekStart)
          AND (:weekEnd IS NULL OR r.weekEndDate <= :weekEnd)
        """)
    Page<WeeklyReport> findForDashboard(
            @Param("userId") UUID userId,
            @Param("projectId") UUID projectId,
            @Param("status") ReportStatus status,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd,
            Pageable pageable);
}
