package com.cadence.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardSummaryResponse {
    private int totalReportsSubmittedThisWeek;
    private SubmissionCompliance compliance;
    private int needsCorrectionCount;
    private int openBlockersCount;
}
