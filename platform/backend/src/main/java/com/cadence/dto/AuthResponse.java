package com.cadence.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

// No token field anymore - it lives only in the httpOnly cookie, never in a
// JSON body JS could read. This is now just "who is logged in" display data.
@Getter
@AllArgsConstructor
public class AuthResponse {
    private UUID userId;
    private String name;
    private String email;
    private String role;
}
