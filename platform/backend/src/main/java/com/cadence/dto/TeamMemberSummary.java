package com.cadence.dto;

import com.cadence.entity.User;
import lombok.Getter;

import java.util.UUID;

@Getter
public class TeamMemberSummary {
    private final UUID userId;
    private final String name;

    public TeamMemberSummary(User user) {
        this.userId = user.getUserId();
        this.name = user.getName();
    }
}
