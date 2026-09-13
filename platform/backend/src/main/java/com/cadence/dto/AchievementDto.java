package com.cadence.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AchievementDto {

    @NotBlank(message = "Description is required")
    private String description;

    private boolean keyAchievement;
}
