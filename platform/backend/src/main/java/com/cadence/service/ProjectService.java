package com.cadence.service;

import com.cadence.dto.ProjectRequest;
import com.cadence.dto.ProjectResponse;
import com.cadence.entity.Project;
import com.cadence.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<ProjectResponse> listAll() {
        return projectRepository.findAll().stream()
                .map(ProjectResponse::new)
                .toList();
    }

    public ProjectResponse create(ProjectRequest request) {
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return new ProjectResponse(projectRepository.save(project));
    }

    public ProjectResponse update(UUID projectId, ProjectRequest request) {
        Project project = findOrThrow(projectId);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        return new ProjectResponse(projectRepository.save(project));
    }

    public void delete(UUID projectId) {
        Project project = findOrThrow(projectId);
        // Deliberately NOT cascading to weekly_reports - a project with existing
        // reports shouldn't silently take them down with it. The FK constraint
        // will reject this delete if reports reference it; that's a feature, not
        // a bug to work around, for a 2-day scope. Surfaced to the user as a 409.
        projectRepository.delete(project);
    }

    private Project findOrThrow(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }
}
