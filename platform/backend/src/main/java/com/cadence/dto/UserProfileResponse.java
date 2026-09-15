package com.cadence.dto;

import com.cadence.entity.User;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UserProfileResponse {
    private final UUID userId;
    private final String name;
    private final String email;
    private final String role;

    public UserProfileResponse(User user) {
        this.userId = user.getUserId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole().getRoleName().name();
    }
}
