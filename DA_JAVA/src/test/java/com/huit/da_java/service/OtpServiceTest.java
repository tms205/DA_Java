package com.huit.da_java.service;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OtpServiceTest {

    @Test
    void issuedOtpCanOnlyBeVerifiedOnce() {
        OtpService service = new OtpService(fixedRandom(), Clock.fixed(Instant.parse("2026-05-24T00:00:00Z"), ZoneId.of("UTC")));

        String code = service.issue("register", "user@example.com");

        assertEquals("123456", code);
        assertEquals(OtpService.VerificationResult.VERIFIED, service.verify("register", "user@example.com", code));
        assertEquals(OtpService.VerificationResult.EXPIRED, service.verify("register", "user@example.com", code));
    }

    @Test
    void otpExpiresAfterFiveMinutes() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-24T00:00:00Z"));
        OtpService service = new OtpService(fixedRandom(), clock);
        String code = service.issue("reset", "user");

        clock.instant = Instant.parse("2026-05-24T00:05:00Z");

        assertEquals(OtpService.VerificationResult.EXPIRED, service.verify("reset", "user", code));
    }

    @Test
    void otpIsBlockedAfterFiveWrongAttempts() {
        OtpService service = new OtpService(fixedRandom(), Clock.systemUTC());
        service.issue("reset", "user");

        for (int i = 0; i < 4; i++) {
            assertEquals(OtpService.VerificationResult.INVALID, service.verify("reset", "user", "000000"));
        }
        assertEquals(OtpService.VerificationResult.TOO_MANY_ATTEMPTS, service.verify("reset", "user", "000000"));
    }

    @Test
    void resendIsLimitedForSixtySeconds() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-24T00:00:00Z"));
        OtpService service = new OtpService(fixedRandom(), clock);
        service.issue("reset", "user");

        assertThrows(IllegalStateException.class, () -> service.issue("reset", "user"));

        clock.instant = Instant.parse("2026-05-24T00:01:00Z");
        assertEquals("123456", service.issue("reset", "user"));
    }

    private SecureRandom fixedRandom() {
        return new SecureRandom() {
            @Override
            public int nextInt(int bound) {
                return 123456;
            }
        };
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
