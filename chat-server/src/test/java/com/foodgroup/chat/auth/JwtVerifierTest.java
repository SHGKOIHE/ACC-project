package com.foodgroup.chat.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtVerifierTest {

    private static final String SECRET = "test-jwt-secret-value";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void verifiesValidTokenAndReturnsMemberId() {
        JwtVerifier verifier = new JwtVerifier(objectMapper, SECRET);
        String token = sign(Map.of("sub", "member-1", "exp", Instant.now().plusSeconds(3600).getEpochSecond()));

        JwtClaims claims = verifier.verify(token);

        assertThat(claims.effectiveMemberId()).isEqualTo("member-1");
    }

    @Test
    void rejectsTokenMissingExpClaim() {
        JwtVerifier verifier = new JwtVerifier(objectMapper, SECRET);
        String token = sign(Map.of("sub", "member-1"));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT missing exp claim");
    }

    @Test
    void rejectsExpiredToken() {
        JwtVerifier verifier = new JwtVerifier(objectMapper, SECRET);
        String token = sign(Map.of("sub", "member-1", "exp", Instant.now().minusSeconds(60).getEpochSecond()));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT expired");
    }

    @Test
    void rejectsUnsupportedAlgorithm() {
        JwtVerifier verifier = new JwtVerifier(objectMapper, SECRET);
        String token = signWithAlg("HS512", Map.of("sub", "member-1", "exp", Instant.now().plusSeconds(3600).getEpochSecond()));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported JWT algorithm: HS512");
    }

    @Test
    void rejectsTamperedSignature() {
        JwtVerifier verifier = new JwtVerifier(objectMapper, SECRET);
        String token = sign(Map.of("sub", "member-1", "exp", Instant.now().plusSeconds(3600).getEpochSecond()));
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> verifier.verify(tampered))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid JWT signature");
    }

    private String sign(Map<String, Object> claims) {
        return signWithAlg("HS256", claims);
    }

    private String signWithAlg(String alg, Map<String, Object> claims) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", alg);
            header.put("typ", "JWT");

            String headerPart = base64Url(objectMapper.writeValueAsBytes(header));
            String payloadPart = base64Url(objectMapper.writeValueAsBytes(claims));
            String signingInput = headerPart + "." + payloadPart;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));

            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
