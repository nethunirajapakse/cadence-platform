package com.cadence.controller;

import com.cadence.dto.auth.AuthResponseDTO;
import com.cadence.dto.auth.LoginRequestDTO;
import com.cadence.dto.auth.RegisterRequestDTO;
import com.cadence.security.CookieUtil;
import com.cadence.security.UserPrincipal;
import com.cadence.service.AuthResult;
import com.cadence.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request, HttpServletResponse response) {
        AuthResult result = authService.register(request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.buildAuthCookie(result.token()).toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result.user());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request, HttpServletResponse response) {
        AuthResult result = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.buildAuthCookie(result.token()).toString());
        return ResponseEntity.ok(result.user());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.clearAuthCookie().toString());
        return ResponseEntity.noContent().build();
    }

    // Lets the frontend ask "am I logged in" without ever touching the token
    // itself - it's httpOnly now, so JS has no way to read or verify it directly.
    // JwtAuthFilter has already populated the principal (or not) by the time this runs.
    @GetMapping("/me")
    public ResponseEntity<AuthResponseDTO> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new AuthResponseDTO(
                principal.getUserId(), principal.getName(), principal.getEmail(), principal.getRoleName()));
    }
}
