package com.huit.da_java.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    private static final Duration OTP_LIFETIME = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom secureRandom;
    private final Clock clock;
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();

    public OtpService() {
        this(new SecureRandom(), Clock.systemUTC());
    }

    OtpService(SecureRandom secureRandom, Clock clock) {
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    public String issue(String purpose, String identity) {
        String key = key(purpose, identity);
        Challenge existing = challenges.get(key);
        if (existing != null && clock.instant().isBefore(existing.issuedAt().plus(RESEND_COOLDOWN))) {
            throw new IllegalStateException("Vui lòng chờ 60 giây trước khi yêu cầu mã OTP mới.");
        }
        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        challenges.put(key, new Challenge(code, clock.instant(), clock.instant().plus(OTP_LIFETIME), 0));
        return code;
    }

    public VerificationResult verify(String purpose, String identity, String submittedCode) {
        String key = key(purpose, identity);
        Challenge challenge = challenges.get(key);
        if (challenge == null || !clock.instant().isBefore(challenge.expiresAt())) {
            challenges.remove(key);
            return VerificationResult.EXPIRED;
        }
        if (challenge.code().equals(submittedCode == null ? "" : submittedCode.trim())) {
            challenges.remove(key);
            return VerificationResult.VERIFIED;
        }

        int attempts = challenge.attempts() + 1;
        if (attempts >= MAX_ATTEMPTS) {
            challenges.remove(key);
            return VerificationResult.TOO_MANY_ATTEMPTS;
        }
        challenges.put(key, new Challenge(challenge.code(), challenge.issuedAt(), challenge.expiresAt(), attempts));
        return VerificationResult.INVALID;
    }

    public void invalidate(String purpose, String identity) {
        challenges.remove(key(purpose, identity));
    }

    private String key(String purpose, String identity) {
        return purpose + ":" + identity.trim().toLowerCase();
    }

    private record Challenge(String code, Instant issuedAt, Instant expiresAt, int attempts) {
    }

    public enum VerificationResult {
        VERIFIED,
        INVALID,
        EXPIRED,
        TOO_MANY_ATTEMPTS
    }
}
