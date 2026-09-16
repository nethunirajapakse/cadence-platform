package com.cadence.repository;

import com.cadence.dto.project.ProjectFilterCriteria;
import com.cadence.entity.Project;

import java.util.List;

public interface ProjectRepositoryCustom {
    List<Project> findByFilters(ProjectFilterCriteria criteria);
}
