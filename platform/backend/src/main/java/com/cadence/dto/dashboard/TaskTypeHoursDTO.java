package com.cadence.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TaskTypeHoursDTO {
    private String taskType;
    private BigDecimal totalHours;
}
