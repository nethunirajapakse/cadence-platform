package com.cadence.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReportRequest {

    @NotNull(message = "Project is required")
    private UUID projectId;

    @NotNull(message = "Week start date is required")
    private java.time.LocalDate weekStartDate;

    @NotNull(message = "Week end date is required")
    private java.time.LocalDate weekEndDate;

    @Size(max = 2000, message = "Notes must be 2000 characters or fewer")
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

    @Valid
    private List<TimeLogDto> timeLogs;

    @Valid
    private List<NoteLinkDto> noteLinks;
}
