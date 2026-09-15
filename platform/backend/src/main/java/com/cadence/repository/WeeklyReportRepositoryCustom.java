package com.cadence.repository;

import com.cadence.dto.ReportFilterCriteria;
import com.cadence.entity.WeeklyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface WeeklyReportRepositoryCustom {
    Page<WeeklyReport> findForDashboard(ReportFilterCriteria criteria, Pageable pageable);

    // Separate from findForDashboard because that method unconditionally
    // excludes DRAFT (managers must never see drafts) - a team member's own
    // history is the opposite: it MUST include their own drafts.
    Page<WeeklyReport> findForOwnHistory(UUID userId, ReportFilterCriteria criteria, Pageable pageable);
}
