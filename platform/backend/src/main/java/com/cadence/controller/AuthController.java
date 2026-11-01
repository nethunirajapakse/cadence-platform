package com.cadence.controller;

import com.cadence.dto.auth.AcceptInviteRequestDTO;
import com.cadence.dto.auth.AuthResponseDTO;
import com.cadence.dto.auth.ForgotPasswordRequestDTO;
import com.cadence.dto.auth.InvitationPreviewDTO;
import com.cadence.dto.auth.LoginRequestDTO;
import com.cadence.dto.auth.ResetPasswordRequestDTO;
import com.cadence.security.CookieUtil;
import com.cadence.security.UserPrincipal;
import com.cadence.service.AuthResult;
import com.cadence.service.AuthService;
import com.cadence.service.InvitationService;
import com.cadence.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// There is deliberately no /register endpoint: accounts are created only by
// accepting an administrator's invitation.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final InvitationService invitationService;
    private final PasswordResetService passwordResetService;
    private final CookieUtil cookieUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        AuthResult result = authService.login(request, httpRequest.getRemoteAddr());
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

    // ---- invitations -------------------------------------------------------

    // Lets the accept-invitation page show who the invitation is for before the
    // invitee fills anything in. 410 Gone when the link is unknown/used/expired.
    @GetMapping("/invitations/preview")
    public InvitationPreviewDTO previewInvitation(@RequestParam String token) {
        return invitationService.preview(token);
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<AuthResponseDTO> acceptInvite(
            @Valid @RequestBody AcceptInviteRequestDTO request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        AuthResult result = invitationService.accept(request, httpRequest.getRemoteAddr());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.buildAuthCookie(result.token()).toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result.user());
    }

    // ---- password reset ------------------------------------------------------

    // 204 whether or not the address has an account.
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request, HttpServletRequest httpRequest) {
        passwordResetService.requestReset(request.getEmail(), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request, HttpServletRequest httpRequest) {
        passwordResetService.confirmReset(request.getToken(), request.getPassword(), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
