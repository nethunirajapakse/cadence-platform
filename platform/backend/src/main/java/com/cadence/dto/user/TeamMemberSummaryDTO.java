package com.cadence.dto.user;

import com.cadence.entity.User;
import lombok.Getter;

import java.util.UUID;

@Getter
public class TeamMemberSummaryDTO {
    private final UUID userId;
    private final String name;

    public TeamMemberSummaryDTO(User user) {
        this.userId = user.getUserId();
        this.name = user.getName();
    }
}
