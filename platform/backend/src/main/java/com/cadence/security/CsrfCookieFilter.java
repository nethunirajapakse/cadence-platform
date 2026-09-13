package com.cadence.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Spring Security's CSRF token is generated lazily - the XSRF-TOKEN cookie only
// actually gets written once something reads CsrfToken.getToken(). Without this
// filter forcing that read on every request, the frontend would never receive
// the cookie at all. This is Spring's own documented recipe for SPA + cookie-based CSRF.
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
        if (csrfToken != null) {
            csrfToken.getToken(); // triggers the deferred token to actually render/save
        }
        filterChain.doFilter(request, response);
    }
}
