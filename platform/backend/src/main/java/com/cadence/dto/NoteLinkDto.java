package com.cadence.dto;

import com.cadence.entity.enums.NoteLinkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteLinkDto {

    @NotNull(message = "Type is required")
    private NoteLinkType type;

    @NotBlank(message = "Content is required")
    private String content;
}
