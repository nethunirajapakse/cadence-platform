package com.cadence.repository;

import com.cadence.entity.WeeklyReport;
import com.cadence.entity.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface WeeklyReportRepositoryCustom {

    // The manager dashboard query - every filter optional and combinable. This is
    // the one built with QueryDSL instead of a JPQL @Query, per your preference:
    // a BooleanBuilder composed conditionally reads more clearly than a single
    // JPQL string with a wall of "(:param IS NULL OR ...)" clauses, and it's
    // type-checked against the entity at compile time via the generated QWeeklyReport.
    Page<WeeklyReport> findForDashboard(
            UUID userId, UUID projectId, ReportStatus status,
            LocalDate weekStart, LocalDate weekEnd, Pageable pageable);
}
