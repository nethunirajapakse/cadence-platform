package com.cadence.service;

import com.cadence.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    // A clock the test can move forward by hand.
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptService service =
            new LoginAttemptService(Duration.ofMinutes(15), Duration.ofMinutes(15), clock);

    @Test
    void locksAfterTheConfiguredNumberOfFailures() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("login-email:a@b.c", 5);
        }
        assertThat(service.isLocked("login-email:a@b.c")).isFalse();

        service.recordFailure("login-email:a@b.c", 5);

        assertThat(service.isLocked("login-email:a@b.c")).isTrue();
        assertThatThrownBy(() -> service.assertNotLocked("login-email:a@b.c"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void unlocksAfterTheLockoutPeriod() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("k", 5);
        }
        assertThat(service.isLocked("k")).isTrue();

        clock.advance(Duration.ofMinutes(16));

        assertThat(service.isLocked("k")).isFalse();
        assertThatCode(() -> service.assertNotLocked("k")).doesNotThrowAnyException();
    }

    @Test
    void failuresOutsideTheWindowDoNotAccumulate() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("k", 5);
        }
        clock.advance(Duration.ofMinutes(20));
        service.recordFailure("k", 5);

        assertThat(service.isLocked("k")).isFalse();
    }

    @Test
    void clearResetsTheCounter() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("k", 5);
        }
        service.clear("k");
        service.recordFailure("k", 5);

        assertThat(service.isLocked("k")).isFalse();
    }

    @Test
    void keysAreIndependent() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("login-email:a@b.c", 5);
        }

        assertThat(service.isLocked("login-email:a@b.c")).isTrue();
        assertThat(service.isLocked("login-email:other@b.c")).isFalse();
    }
}
