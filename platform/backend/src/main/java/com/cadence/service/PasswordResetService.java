package com.cadence.service;

import com.cadence.entity.PasswordResetToken;
import com.cadence.entity.User;
import com.cadence.entity.enums.AuditAction;
import com.cadence.exception.ApiException;
import com.cadence.repository.user.PasswordResetTokenRepository;
import com.cadence.repository.user.UserRepository;
import com.cadence.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final String INVALID_LINK_MESSAGE =
            "This password reset link is invalid or has expired. Request a new one.";
    private static final int MAX_REQUESTS_PER_EMAIL = 5;
    private static final int MAX_REQUESTS_PER_IP = 20;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final MailService mailService;
    private final AuditService auditService;
    private final LoginAttemptService attemptService;

    @Value("${app.security.reset-ttl-minutes:30}")
    private long resetTtlMinutes;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // Always completes normally for a well-formed request, whether or not the
    // address belongs to an account, so the endpoint cannot be used to find out
    // who has one.
    @Transactional
    public void requestReset(String rawEmail, String ip) {
        String email = TokenUtil.normalizeEmail(rawEmail);
        String ipKey = "reset-ip:" + ip;
        String emailKey = "reset-email:" + email;

        attemptService.assertNotLocked(ipKey);
        attemptService.recordFailure(ipKey, MAX_REQUESTS_PER_IP);
        if (attemptService.isLocked(emailKey)) {
            return; // silently: the caller must not learn anything about this address
        }
        attemptService.recordFailure(emailKey, MAX_REQUESTS_PER_EMAIL);

        Optional<User> found = userRepository.findByEmail(email).filter(User::isActive);

        auditService.record(found.map(User::getUserId).orElse(null), email,
                AuditAction.PASSWORD_RESET_REQUESTED, email, null, ip);

        if (found.isEmpty()) {
            return;
        }
        User user = found.get();

        // Only the newest link works.
        LocalDateTime now = LocalDateTime.now();
        for (PasswordResetToken old : tokenRepository.findUnusedByUserId(user.getUserId())) {
            old.setUsedAt(now);
        }

        String token = TokenUtil.newToken();
        tokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(TokenUtil.hash(token))
                .expiresAt(now.plusMinutes(resetTtlMinutes))
                .build());

        try {
            mailService.sendPasswordReset(user.getEmail(), linkFor(token), resetTtlMinutes);
        } catch (ApiException e) {
            // Don't let a mail failure turn into a different response for real accounts.
            log.error("Password reset email could not be sent to {}", user.getEmail());
        }
    }

    @Transactional
    public void confirmReset(String token, String newPassword, String ip) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.GONE, INVALID_LINK_MESSAGE);
        }
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(TokenUtil.hash(token.trim()))
                .orElseThrow(() -> new ApiException(HttpStatus.GONE, INVALID_LINK_MESSAGE));

        LocalDateTime now = LocalDateTime.now();
        User user = resetToken.getUser();
        if (!resetToken.isUsable(now) || !user.isActive()) {
            throw new ApiException(HttpStatus.GONE, INVALID_LINK_MESSAGE);
        }

        passwordPolicy.validate(newPassword, user.getEmail(), user.getName());

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Every session issued before this moment stops working (see JwtAuthFilter).
        user.setPasswordChangedAt(now);
        userRepository.save(user);

        resetToken.setUsedAt(now);
        for (PasswordResetToken other : tokenRepository.findUnusedByUserId(user.getUserId())) {
            other.setUsedAt(now);
        }

        // A successful reset also lifts any login lockout on this address.
        attemptService.clear("login-email:" + TokenUtil.normalizeEmail(user.getEmail()));

        auditService.record(user.getUserId(), user.getEmail(), AuditAction.PASSWORD_RESET_COMPLETED,
                user.getEmail(), null, ip);
    }

    private String linkFor(String token) {
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return base + "/reset-password?token=" + token;
    }
}
