package com.cadence.dto;

import com.cadence.entity.enums.TaskType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TimeLogDto {

    @NotNull(message = "Task type is required")
    private TaskType taskType;

    @NotNull(message = "Hours is required")
    private BigDecimal hours;
}
