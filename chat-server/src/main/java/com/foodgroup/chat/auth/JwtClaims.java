package com.foodgroup.chat.auth;

import java.time.Instant;
import java.util.Map;

public record JwtClaims(
        String subject,
        String memberId,
        Instant expiresAt,
        Map<String, Object> claims
) {
    public String effectiveMemberId() {
        if (memberId != null && !memberId.isBlank()) {
            return memberId;
        }
        return subject;
    }
}
