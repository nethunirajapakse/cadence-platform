package com.cadence.service;

import com.cadence.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

// In-memory failure counter with temporary lockout. Good for a single backend
// instance; running several instances would need this moved to a shared store
// (e.g. Redis) so the counters are shared.
//
// Keys are opaque strings chosen by the caller ("login-email:a@b.c",
// "login-ip:1.2.3.4", ...), so one instance serves every throttled flow.
@Service
public class LoginAttemptService {

    private static final int PURGE_THRESHOLD = 5_000;

    private static final class Entry {
        int failures;
        long windowStartMs;
        long lockedUntilMs;
    }

    private final Duration window;
    private final Duration lockout;
    private final Clock clock;
    private final Map<String, Entry> entries = new HashMap<>();

    @Autowired
    public LoginAttemptService(
            @Value("${app.security.attempt-window-minutes:15}") long windowMinutes,
            @Value("${app.security.lockout-minutes:15}") long lockoutMinutes) {
        this(Duration.ofMinutes(windowMinutes), Duration.ofMinutes(lockoutMinutes), Clock.systemUTC());
    }

    LoginAttemptService(Duration window, Duration lockout, Clock clock) {
        this.window = window;
        this.lockout = lockout;
        this.clock = clock;
    }

    public synchronized boolean isLocked(String key) {
        Entry entry = entries.get(key);
        return entry != null && entry.lockedUntilMs > now();
    }

    public synchronized void assertNotLocked(String key) {
        if (isLocked(key)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts. Please wait a few minutes and try again.");
        }
    }

    // Counts one failure; once the count within the window reaches maxFailures
    // the key is locked for the lockout period.
    public synchronized void recordFailure(String key, int maxFailures) {
        long now = now();
        if (entries.size() > PURGE_THRESHOLD) {
            purgeStale(now);
        }

        Entry entry = entries.computeIfAbsent(key, k -> new Entry());
        if (entry.windowStartMs == 0 || now - entry.windowStartMs > window.toMillis()) {
            entry.windowStartMs = now;
            entry.failures = 0;
        }
        entry.failures++;
        if (entry.failures >= maxFailures) {
            entry.lockedUntilMs = now + lockout.toMillis();
            entry.failures = 0;
            entry.windowStartMs = 0;
        }
    }

    public synchronized void clear(String key) {
        entries.remove(key);
    }

    private void purgeStale(long now) {
        entries.values().removeIf(e ->
                e.lockedUntilMs <= now && (e.windowStartMs == 0 || now - e.windowStartMs > window.toMillis()));
    }

    private long now() {
        return clock.millis();
    }
}
