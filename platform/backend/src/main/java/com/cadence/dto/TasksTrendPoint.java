package com.cadence.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class TasksTrendPoint {
    private LocalDate weekStartDate;
    private int tasksCompleted;
}
