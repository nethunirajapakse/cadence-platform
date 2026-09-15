package com.cadence.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    public static final String COOKIE_NAME = "auth_token";

    private final boolean secure;
    private final String sameSite;
    private final long maxAgeMs;

    public CookieUtil(
            // false for local http dev - set app.jwt.cookie-secure=true once deployed behind HTTPS,
            // otherwise browsers silently refuse to store/send the cookie at all.
            @Value("${app.jwt.cookie-secure:false}") boolean secure,
            // Lax for local dev (frontend/backend on localhost = same site).
            // Set app.jwt.cookie-samesite=None once frontend and backend are on
            // different domains (e.g. Vercel + Render) - Lax blocks the cookie
            // on cross-site fetch/XHR requests, only Strict/Lax navigations work.
            // None requires secure=true or browsers reject the cookie outright.
            @Value("${app.jwt.cookie-samesite:Lax}") String sameSite,
            @Value("${app.jwt.expiration-ms}") long maxAgeMs) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAgeMs = maxAgeMs;
    }

    public ResponseCookie buildAuthCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)   // the entire point - JS cannot read this, so it can't be stolen via XSS
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
