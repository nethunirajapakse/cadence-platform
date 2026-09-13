package com.cadence.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubmissionCompliance {
    private int submitted;
    private int pending;   // week still open, no report yet (or still a draft)
    private int late;      // week already ended, still no report (or still a draft)
    private int totalExpected;
}
