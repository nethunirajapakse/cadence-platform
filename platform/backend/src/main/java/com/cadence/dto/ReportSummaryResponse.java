package com.cadence.dto;

import com.cadence.entity.WeeklyReport;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// Lighter shape for list views (own history, manager dashboard) - no child
// collections, so listing 50 reports doesn't drag along their full task tables.
@Getter
public class ReportSummaryResponse {
    private final UUID reportId;
    private final String userName;
    private final String projectName;
    private final LocalDate weekStartDate;
    private final LocalDate weekEndDate;
    private final String status;
    private final LocalDateTime submittedAt;

    public ReportSummaryResponse(WeeklyReport report) {
        this.reportId = report.getReportId();
        this.userName = report.getUser().getName();
        this.projectName = report.getProject().getName();
        this.weekStartDate = report.getWeekStartDate();
        this.weekEndDate = report.getWeekEndDate();
        this.status = report.getStatus().name();
        this.submittedAt = report.getSubmittedAt();
    }
}
