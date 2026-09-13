package com.cadence.dto;

import com.cadence.entity.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NextWeekTaskDto {

    @NotBlank(message = "Task description is required")
    private String taskDescription;

    private Priority priority;
}
