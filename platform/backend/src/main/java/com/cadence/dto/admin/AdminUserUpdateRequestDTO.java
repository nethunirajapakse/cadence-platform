package com.cadence.dto.admin;

import com.cadence.entity.enums.RoleName;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// Partial update: any field left null is left unchanged.
@Getter
@Setter
public class AdminUserUpdateRequestDTO {

    @Size(max = 120, message = "Name must be at most 120 characters")
    private String name;

    private RoleName role;

    // Only meaningful for TEAM_MEMBER; cleared automatically for other roles.
    private UUID managerId;
}
