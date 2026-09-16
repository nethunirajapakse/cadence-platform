package com.cadence.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubmissionComplianceDTO {
    private int submitted;
    private int pending;   // week still open, no report yet (or still a draft)
    private int late;      // week already ended, still no report (or still a draft)
    private int totalExpected;
}
