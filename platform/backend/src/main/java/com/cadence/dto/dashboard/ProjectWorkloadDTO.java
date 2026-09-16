package com.cadence.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectWorkloadDTO {
    private String projectName;
    private int taskCount;
}
