package com.cadence.config.seed;

import com.cadence.dto.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SeedReport {
    private String userEmail;
    private String projectName;
    private int weeksAgo;
    private String notes;
    private List<ReportTaskDto> tasks;
    private List<NextWeekTaskDto> nextWeekTasks;
    private List<BlockerDto> blockers;
    private List<AchievementDto> achievements;
    private List<TimeLogDto> timeLogs;
    private List<NoteLinkDto> noteLinks;

    // "DRAFT" | "SUBMITTED" | "NEEDS_CORRECTION" | "APPROVED" | "APPROVED_AFTER_CORRECTION"
    // The last one simulates a full two-round review cycle: submit -> request
    // changes -> resubmit -> approve, so the seeded data actually demonstrates
    // multi-version history, not just single-version end states.
    private String targetStatus;
    private String reviewComment;
    private String approvalComment;
}
