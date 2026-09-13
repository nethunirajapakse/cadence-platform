package com.cadence.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TaskTypeHours {
    private String taskType;
    private BigDecimal totalHours;
}
