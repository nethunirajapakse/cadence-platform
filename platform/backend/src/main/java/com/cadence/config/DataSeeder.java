package com.cadence.config;

import com.cadence.entity.Role;
import com.cadence.entity.enums.RoleName;
import com.cadence.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Since this project uses ddl-auto=update instead of Flyway, there's no migration
// to seed reference data. This runs once on every startup and only inserts what's
// missing, so it's safe to leave in permanently (idempotent, not a one-off script).
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        seedRoleIfMissing(RoleName.TEAM_MEMBER, "Creates and submits their own weekly reports");
        seedRoleIfMissing(RoleName.MANAGER, "Reviews reports across the whole team and manages projects");
    }

    private void seedRoleIfMissing(RoleName roleName, String description) {
        roleRepository.findByRoleName(roleName).orElseGet(() ->
                roleRepository.save(Role.builder()
                        .roleName(roleName)
                        .description(description)
                        .build())
        );
    }
}
