package com.cadence.dto;

import com.cadence.entity.enums.Priority;
import com.cadence.entity.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReportTaskDto {

    @NotBlank(message = "Task name is required")
    @Size(max = 200, message = "Task name must be 200 characters or fewer")
    private String taskName;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private Integer plannedPct;
    private Integer actualPct;

    @NotNull(message = "Status is required")
    private TaskStatus status;

    private BigDecimal timePlanned;
    private BigDecimal timeSpent;

    @Size(max = 300, message = "Deliverable must be 300 characters or fewer")
    private String deliverable;
}
