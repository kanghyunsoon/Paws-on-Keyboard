package com.tour_diary.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {

    private final String secret;

    public AuthTokenService(@Value("${app.auth.token-secret:${APP_AUTH_TOKEN_SECRET:local-dev-token-secret}}") String secret) {
        this.secret = secret;
    }

    public TokenIssue issue(String userId) {
        Instant expiresAt = Instant.now().plus(14, ChronoUnit.DAYS);
        String payload = userId + ":" + expiresAt.getEpochSecond();
        String encodedPayload = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return new TokenIssue(encodedPayload + "." + signature, expiresAt);
    }

    public String verifyAndGetUserId(String token) {
        if (token == null || token.isBlank() || !token.contains(".")) {
            throw new IllegalArgumentException("Invalid token");
        }
        String[] parts = token.split("\\.", 2);
        if (!sign(parts[0]).equals(parts[1])) {
            throw new IllegalArgumentException("Invalid token signature");
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String[] values = payload.split(":", 2);
        if (values.length != 2) {
            throw new IllegalArgumentException("Invalid token payload");
        }
        long expiresAt = Long.parseLong(values[1]);
        if (Instant.now().isAfter(Instant.ofEpochSecond(expiresAt))) {
            throw new IllegalArgumentException("Expired token");
        }
        return values[0];
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot sign auth token", ex);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record TokenIssue(String token, Instant expiresAt) {
    }
}
