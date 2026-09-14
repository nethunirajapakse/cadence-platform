package com.cadence.repository;

import com.cadence.dto.ProjectFilterCriteria;
import com.cadence.entity.Project;

import java.util.List;

public interface ProjectRepositoryCustom {
    List<Project> findByFilters(ProjectFilterCriteria criteria);
}
