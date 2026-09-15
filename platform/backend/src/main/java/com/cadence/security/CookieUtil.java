package com.cadence.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    public static final String COOKIE_NAME = "auth_token";

    private final boolean secure;
    private final long maxAgeMs;

    public CookieUtil(
            // false for local http dev - set app.jwt.cookie-secure=true once deployed behind HTTPS,
            // otherwise browsers silently refuse to store/send the cookie at all.
            @Value("${app.jwt.cookie-secure:false}") boolean secure,
            @Value("${app.jwt.expiration-ms}") long maxAgeMs) {
        this.secure = secure;
        this.maxAgeMs = maxAgeMs;
    }

    public ResponseCookie buildAuthCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)   // the entire point - JS cannot read this, so it can't be stolen via XSS
                .secure(secure)
                .sameSite("Lax")  // survives normal navigation/links; blocks it being sent on cross-site POSTs
                .path("/")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .build();
    }

    public ResponseCookie clearAuthCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}
