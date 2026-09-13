package com.cadence.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UUID userId;
    private String name;
    private String email;
    private String role;
}
