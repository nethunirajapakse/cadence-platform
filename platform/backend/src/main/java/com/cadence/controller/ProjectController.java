package com.cadence.controller;

import com.cadence.dto.ProjectFilterCriteria;
import com.cadence.dto.ProjectRequestDTO;
import com.cadence.dto.ProjectResponseDTO;
import com.cadence.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // criteria is populated by Spring MVC's implicit @ModelAttribute binding,
    // same pattern as ReportController's dashboard endpoint - ?name=foo&
    // description=bar map straight onto ProjectFilterCriteria's fields.
    @GetMapping
    public List<ProjectResponseDTO> list(ProjectFilterCriteria criteria) {
        return projectService.listAll(criteria);
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ProjectResponseDTO> create(@Valid @RequestBody ProjectRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ProjectResponseDTO update(@PathVariable UUID projectId, @Valid @RequestBody ProjectRequestDTO request) {
        return projectService.update(projectId, request);
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable UUID projectId) {
        projectService.delete(projectId);
        return ResponseEntity.noContent().build();
    }
}
