package com.cadence.dto.report;

import com.cadence.dto.report.item.*;
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
public class ReportRequestDTO {

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
    private List<ReportTaskDTO> tasks;

    @Valid
    private List<NextWeekTaskDTO> nextWeekTasks;

    @Valid
    private List<BlockerDTO> blockers;

    @Valid
    private List<AchievementDTO> achievements;

    @Valid
    private List<TimeLogDTO> timeLogs;

    @Valid
    private List<NoteLinkDTO> noteLinks;
}
