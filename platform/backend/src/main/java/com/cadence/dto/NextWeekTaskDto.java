package com.cadence.dto;

import com.cadence.entity.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NextWeekTaskDto {

    @NotBlank(message = "Task description is required")
    @Size(max = 300, message = "Task description must be 300 characters or fewer")
    private String taskDescription;

    private Priority priority;
}
