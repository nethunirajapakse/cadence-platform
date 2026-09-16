package com.cadence.service;

import com.cadence.dto.auth.AuthResponseDTO;

// Internal to the service/controller boundary only - never serialized as a JSON
// response body. The controller reads token() to set the cookie header, and
// sends user() (the token-free AuthResponse) as the actual response body.
public record AuthResult(String token, AuthResponseDTO user) {
}
