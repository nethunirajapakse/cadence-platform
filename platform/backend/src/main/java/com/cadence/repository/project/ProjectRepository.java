package com.cadence.repository.project;

import com.cadence.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID>, ProjectRepositoryCustom {
    Optional<Project> findByName(String name);
}
