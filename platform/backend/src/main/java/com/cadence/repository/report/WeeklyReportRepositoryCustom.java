package com.cadence.repository.report;

import com.cadence.dto.report.ReportFilterCriteria;
import com.cadence.entity.WeeklyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.UUID;

public interface WeeklyReportRepositoryCustom {
    Page<WeeklyReport> findForDashboard(ReportFilterCriteria criteria, Collection<UUID> allowedUserIds, Pageable pageable);

    Page<WeeklyReport> findForOwnHistory(UUID userId, ReportFilterCriteria criteria, Pageable pageable);
}
