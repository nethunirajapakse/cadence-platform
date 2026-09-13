package com.cadence.dto;

import com.cadence.entity.Project;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ProjectResponse {
    private final UUID projectId;
    private final String name;
    private final String description;

    public ProjectResponse(Project project) {
        this.projectId = project.getProjectId();
        this.name = project.getName();
        this.description = project.getDescription();
    }
}
