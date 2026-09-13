package com.cadence.repository;

import com.cadence.entity.WeeklyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, UUID>, WeeklyReportRepositoryCustom {

    // Team member's own paginated history - a plain derived query, no dynamic
    // filtering needed here, so this stays as-is rather than going through QueryDSL too.
    Page<WeeklyReport> findByUser_UserId(UUID userId, Pageable pageable);
}
