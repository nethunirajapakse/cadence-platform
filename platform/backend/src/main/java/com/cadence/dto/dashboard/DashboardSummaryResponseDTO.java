package com.cadence.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardSummaryResponseDTO {
    private int totalReportsSubmittedThisWeek;
    private SubmissionComplianceDTO compliance;
    private int needsCorrectionCount;
    private int openBlockersCount;
}
