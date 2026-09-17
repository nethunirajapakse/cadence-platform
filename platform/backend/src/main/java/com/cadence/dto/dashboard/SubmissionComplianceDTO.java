package com.cadence.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubmissionComplianceDTO {
    private int submitted;
    private int pending;
    private int late;
    private int totalExpected;
}
