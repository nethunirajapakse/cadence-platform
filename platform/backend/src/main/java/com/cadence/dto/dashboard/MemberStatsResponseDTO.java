package com.cadence.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberStatsResponseDTO {
    private int totalReports;       // excluding DRAFT - a manager can't see those anyway
    private int approvedCount;
    private int needsCorrectionCount;
    private int tasksCompletedCount;
    private int openBlockersCount;  // blockers on SUBMITTED/NEEDS_CORRECTION reports only
}
