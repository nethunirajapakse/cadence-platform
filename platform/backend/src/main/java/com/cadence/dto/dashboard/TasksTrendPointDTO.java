package com.cadence.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class TasksTrendPointDTO {
    private LocalDate weekStartDate;
    private int tasksCompleted;
}
