package com.cadence.config;

import com.cadence.entity.Project;
import com.cadence.entity.Role;
import com.cadence.entity.enums.RoleName;
import com.cadence.repository.ProjectRepository;
import com.cadence.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final ProjectRepository projectRepository;

    @Override
    public void run(String... args) {
        seedRoleIfMissing(RoleName.TEAM_MEMBER, "Creates and submits their own weekly reports");
        seedRoleIfMissing(RoleName.MANAGER, "Reviews reports across the whole team and manages projects");

        seedProjectIfMissing("Client A", "Ongoing client engagement work");
        seedProjectIfMissing("Internal Tooling", "Internal platform and dev-experience work");
    }

    private void seedRoleIfMissing(RoleName roleName, String description) {
        roleRepository.findByRoleName(roleName).orElseGet(() ->
                roleRepository.save(Role.builder()
                        .roleName(roleName)
                        .description(description)
                        .build())
        );
    }

    // Projects have no unique constraint on name, so "missing" here just means
    // "no project with this exact name yet" - fine for a seeder that only ever
    // runs against a fresh dev database, not a check for real dedup logic.
    private void seedProjectIfMissing(String name, String description) {
        boolean exists = projectRepository.findAll().stream()
                .anyMatch(project -> project.getName().equals(name));

        if (!exists) {
            projectRepository.save(Project.builder()
                    .name(name)
                    .description(description)
                    .build());
        }
    }
}
