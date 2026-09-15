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
    // SameSite=None is required once frontend and backend are on different
    // registrable domains (Vercel + Render) - browsers never attach a Lax or
    // Strict cookie to a cross-site fetch/XHR, only to top-level navigations,
    // so every API call after login would look anonymous without this.
    // None requires Secure=true or browsers reject the cookie outright, which
    // is exactly why this is tied to the same flag rather than a separate one:
    // the two attributes are only ever valid together in this app's two
    // environments (local http -> Lax, deployed https -> None).
    private final String sameSite;

    public CookieUtil(
            @Value("${app.jwt.cookie-secure:false}") boolean secure,
            @Value("${app.jwt.expiration-ms}") long maxAgeMs) {
        this.secure = secure;
        this.maxAgeMs = maxAgeMs;
        this.sameSite = secure ? "None" : "Lax";
    }

    public ResponseCookie buildAuthCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .build();
    }

    public ResponseCookie clearAuthCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();
    }
}
