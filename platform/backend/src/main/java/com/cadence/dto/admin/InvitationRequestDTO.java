package com.cadence.dto.admin;

import com.cadence.entity.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class InvitationRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 180, message = "Email must be at most 180 characters")
    private String email;

    @NotNull(message = "Role is required")
    private RoleName role;

    // Required when role is TEAM_MEMBER, ignored otherwise.
    private UUID managerId;
}
