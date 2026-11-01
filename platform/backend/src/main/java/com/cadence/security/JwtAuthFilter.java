package com.cadence.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromCookie(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticate(token, request);
        } catch (JwtException | IllegalArgumentException e) {
            // Tampered, malformed or expired token: treat the request as anonymous
            // (the endpoint's own rules then answer 401) instead of failing with a 500.
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        String email = jwtService.extractEmail(token);

        if (email == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            return;
        }

        if (!jwtService.isTokenValid(token, userDetails.getUsername())) {
            return;
        }

        // Checked on every request, not just at login, so deactivating a user
        // locks them out immediately instead of when their token expires.
        if (!userDetails.isEnabled()) {
            return;
        }

        // A password change/reset invalidates every session issued before it.
        if (userDetails instanceof UserPrincipal principal && principal.getPasswordChangedAt() != null) {
            Instant changedAt = principal.getPasswordChangedAt()
                    .truncatedTo(ChronoUnit.SECONDS)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
            Instant issuedAt = jwtService.extractIssuedAt(token).toInstant();
            if (issuedAt.isBefore(changedAt)) {
                return;
            }
        }

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    // Reads from the httpOnly cookie now, not an Authorization header - the
    // browser attaches it automatically, so there's no header for JS to set.
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (CookieUtil.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
