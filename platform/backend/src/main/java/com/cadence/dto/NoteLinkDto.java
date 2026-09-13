package com.cadence.dto;

import com.cadence.entity.enums.NoteLinkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteLinkDto {

    @NotNull(message = "Type is required")
    private NoteLinkType type;

    @NotBlank(message = "Content is required")
    @Size(max = 1000, message = "Content must be 1000 characters or fewer")
    private String content;
}
