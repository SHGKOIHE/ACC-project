package com.foodgroup.chat.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtVerifier {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final byte[] secret;

    public JwtVerifier(
            ObjectMapper objectMapper,
            @Value("${chat.auth.jwt-secret:${JWT_SECRET:dev-secret}}") String jwtSecret
    ) {
        this.objectMapper = objectMapper;
        this.secret = decodeSecret(jwtSecret);
    }

    public JwtClaims verify(String rawToken) {
        String token = stripBearerPrefix(rawToken);
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        Map<String, Object> header = readJson(parts[0]);
        String alg = String.valueOf(header.get("alg"));
        if (!"HS256".equals(alg)) {
            throw new IllegalArgumentException("Unsupported JWT algorithm: " + alg);
        }

        byte[] expected = hmacSha256(parts[0] + "." + parts[1]);
        byte[] actual = base64UrlDecode(parts[2]);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new IllegalArgumentException("Invalid JWT signature");
        }

        Map<String, Object> claims = readJson(parts[1]);
        Instant expiresAt = readExpiry(claims);
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("JWT expired");
        }

        String subject = stringClaim(claims, "sub");
        String memberId = stringClaim(claims, "memberId");
        if ((subject == null || subject.isBlank()) && (memberId == null || memberId.isBlank())) {
            throw new IllegalArgumentException("JWT subject/memberId missing");
        }

        return new JwtClaims(subject, memberId, expiresAt, claims);
    }

    private Map<String, Object> readJson(String encoded) {
        try {
            return objectMapper.readValue(base64UrlDecode(encoded), MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT JSON", e);
        }
    }

    private byte[] hmacSha256(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("JWT HMAC verification failed", e);
        }
    }

    private static Instant readExpiry(Map<String, Object> claims) {
        Object exp = claims.get("exp");
        if (exp instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }
        if (exp instanceof String value && !value.isBlank()) {
            return Instant.ofEpochSecond(Long.parseLong(value));
        }
        return null;
    }

    private static String stringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        return value == null ? null : String.valueOf(value);
    }

    private static byte[] decodeSecret(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be blank");
        }
        if (value.startsWith("base64:")) {
            return Base64.getDecoder().decode(value.substring("base64:".length()));
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String stripBearerPrefix(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("JWT missing");
        }
        String trimmed = rawToken.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return trimmed.substring("Bearer ".length()).trim();
        }
        return trimmed;
    }

    private static byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
