package com.cadence.repository;

import com.cadence.entity.Role;
import com.cadence.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByRoleName(RoleName roleName);
}
