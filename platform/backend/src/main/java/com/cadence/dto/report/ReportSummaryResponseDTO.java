package com.cadence.dto.report;

import com.cadence.entity.WeeklyReport;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ReportSummaryResponseDTO {
    private final UUID reportId;
    private final UUID userId;
    private final String userName;
    private final String projectName;
    private final LocalDate weekStartDate;
    private final LocalDate weekEndDate;
    private final String status;
    private final LocalDateTime submittedAt;

    public ReportSummaryResponseDTO(WeeklyReport report) {
        this.reportId = report.getReportId();
        this.userId = report.getUser().getUserId();
        this.userName = report.getUser().getName();
        this.projectName = report.getProject().getName();
        this.weekStartDate = report.getWeekStartDate();
        this.weekEndDate = report.getWeekEndDate();
        this.status = report.getStatus().name();
        this.submittedAt = report.getSubmittedAt();
    }
}
