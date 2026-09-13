package com.cadence.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectWorkload {
    private String projectName;
    private int taskCount;
}
