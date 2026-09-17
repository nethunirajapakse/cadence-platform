package com.cadence.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuthResponseDTO {
    private UUID userId;
    private String name;
    private String email;
    private String role;
}
