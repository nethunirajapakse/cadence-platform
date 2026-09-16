package com.cadence.service;

import com.cadence.dto.project.ProjectFilterCriteria;
import com.cadence.dto.project.ProjectRequestDTO;
import com.cadence.dto.project.ProjectResponseDTO;
import com.cadence.entity.Project;
import com.cadence.repository.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<ProjectResponseDTO> listAll(ProjectFilterCriteria criteria) {
        return projectRepository.findByFilters(criteria).stream()
                .map(ProjectResponseDTO::new)
                .toList();
    }

    public ProjectResponseDTO create(ProjectRequestDTO request) {
        assertNameAvailable(request.getName(), null);

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return new ProjectResponseDTO(projectRepository.save(project));
    }

    public ProjectResponseDTO update(UUID projectId, ProjectRequestDTO request) {
        Project project = findOrThrow(projectId);
        assertNameAvailable(request.getName(), projectId);

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        return new ProjectResponseDTO(projectRepository.save(project));
    }

    public void delete(UUID projectId) {
        Project project = findOrThrow(projectId);
        // Deliberately NOT cascading to weekly_reports - a project with existing
        // reports shouldn't silently take them down with it. The FK constraint
        // will reject this delete if reports reference it; that's a feature, not
        // a bug to work around, for a 2-day scope. Surfaced to the user as a 409.
        projectRepository.delete(project);
    }

    private void assertNameAvailable(String name, UUID excludeProjectId) {
        projectRepository.findByName(name).ifPresent(existing -> {
            if (excludeProjectId == null || !existing.getProjectId().equals(excludeProjectId)) {
                throw new IllegalStateException("A project named \"" + name + "\" already exists");
            }
        });
    }

    private Project findOrThrow(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }
}
