package com.cadence.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberStatsResponseDTO {
    private int totalReports;
    private int approvedCount;
    private int needsCorrectionCount;
    private int tasksCompletedCount;
    private int openBlockersCount;
}
