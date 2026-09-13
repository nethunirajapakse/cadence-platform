package com.cadence.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

// Used for both create (draft) and update (edit while DRAFT/NEEDS_CORRECTION) -
// the fixed field set the assignment requires, identical for every user.
@Getter
@Setter
public class ReportRequest {

    @NotNull(message = "Project is required")
    private UUID projectId;

    @NotNull(message = "Week start date is required")
    private java.time.LocalDate weekStartDate;

    @NotNull(message = "Week end date is required")
    private java.time.LocalDate weekEndDate;

    private String notes;

    @NotEmpty(message = "At least one task is required")
    @Valid
    private List<ReportTaskDto> tasks;

    @Valid
    private List<NextWeekTaskDto> nextWeekTasks;

    @Valid
    private List<BlockerDto> blockers;

    @Valid
    private List<AchievementDto> achievements;

    // Hours by task type - optional per the spec.
    @Valid
    private List<TimeLogDto> timeLogs;

    @Valid
    private List<NoteLinkDto> noteLinks;
}
