package com.cadence.dto.report.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockerDTO {

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must be 500 characters or fewer")
    private String description;

    private boolean keyIssue;
}
