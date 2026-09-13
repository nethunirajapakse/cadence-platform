package com.cadence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditCommentRequest {

    @NotBlank(message = "Comment is required")
    @Size(max = 1000, message = "Comment must be 1000 characters or fewer")
    private String comment;
}