package com.cadence.dto.report.item;

import com.cadence.entity.enums.TaskType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TimeLogDTO {

    @NotNull(message = "Task type is required")
    private TaskType taskType;

    @NotNull(message = "Hours is required")
    private BigDecimal hours;
}
