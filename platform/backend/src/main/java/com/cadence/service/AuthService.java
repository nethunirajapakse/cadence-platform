package com.cadence.service;

import com.cadence.dto.auth.AuthResponseDTO;
import com.cadence.dto.auth.LoginRequestDTO;
import com.cadence.entity.User;
import com.cadence.entity.enums.AuditAction;
import com.cadence.exception.ApiException;
import com.cadence.repository.user.UserRepository;
import com.cadence.security.JwtService;
import com.cadence.security.UserPrincipal;
import com.cadence.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

// Accounts are no longer created here: they come into existence only through
// InvitationService.accept (invite link) or the startup bootstrap/seed in
// DataSeeder. This class only signs existing users in.
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService attemptService;
    private final AuditService auditService;

    @Value("${app.security.max-failed-logins:5}")
    private int maxFailedLogins;

    public AuthResult login(LoginRequestDTO request, String ip) {
        String email = TokenUtil.normalizeEmail(request.getEmail());
        String emailKey = "login-email:" + email;
        String ipKey = "login-ip:" + ip;

        try {
            attemptService.assertNotLocked(emailKey);
            attemptService.assertNotLocked(ipKey);
        } catch (ApiException blocked) {
            auditService.record(null, email, AuditAction.LOGIN_BLOCKED, email, null, ip);
            throw blocked;
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (AuthenticationException failure) {
            attemptService.recordFailure(emailKey, maxFailedLogins);
            // The per-address limit is the tight one; the per-IP limit is looser so
            // one shared office/NAT address is not locked out by a few typos.
            attemptService.recordFailure(ipKey, maxFailedLogins * 4);
            auditService.record(null, email, AuditAction.LOGIN_FAILED, email, null, ip);
            throw failure;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found after successful authentication"));

        attemptService.clear(emailKey);
        auditService.record(user.getUserId(), user.getEmail(), AuditAction.LOGIN_SUCCESS, user.getEmail(), null, ip);

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);
        AuthResponseDTO userResponse = new AuthResponseDTO(
                user.getUserId(), user.getName(), user.getEmail(), user.getRole().getRoleName().name());

        return new AuthResult(token, userResponse);
    }
}
