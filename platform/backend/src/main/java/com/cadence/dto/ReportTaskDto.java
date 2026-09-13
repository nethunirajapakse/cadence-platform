package com.cadence.dto;

import com.cadence.entity.enums.Priority;
import com.cadence.entity.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReportTaskDto {

    @NotBlank(message = "Task name is required")
    private String taskName;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private Integer plannedPct;
    private Integer actualPct;

    @NotNull(message = "Status is required")
    private TaskStatus status;

    private BigDecimal timePlanned;
    private BigDecimal timeSpent;
    private String deliverable;
}
