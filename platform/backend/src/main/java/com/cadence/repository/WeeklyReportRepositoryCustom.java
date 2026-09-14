package com.cadence.repository;

import com.cadence.dto.ReportFilterCriteria;
import com.cadence.entity.WeeklyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WeeklyReportRepositoryCustom {
    Page<WeeklyReport> findForDashboard(ReportFilterCriteria criteria, Pageable pageable);
}
