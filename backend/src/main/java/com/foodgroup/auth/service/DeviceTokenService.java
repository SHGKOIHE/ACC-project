package com.foodgroup.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final StringRedisTemplate redisTemplate;

    @Value("${auth.device-token.key-prefix:${AUTH_DEVICE_TOKEN_KEY_PREFIX:device-token:}}")
    private String keyPrefix;

    @Value("${auth.device-token.ttl-hours:${AUTH_DEVICE_TOKEN_TTL_HOURS:24}}")
    private long ttlHours;

    public String issueToken(String memberId) {
        String token = generateToken();
        redisTemplate.opsForValue().set(redisKey(token), memberId, Duration.ofHours(ttlHours));
        return token;
    }

    public Optional<String> resolveMemberId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String key = redisKey(token.trim());
        Optional<String> memberId = Optional.ofNullable(redisTemplate.opsForValue().get(key))
                .filter(id -> !id.isBlank());
        // Sliding expiration: an active user's session never hits the hard TTL; an inactive
        // one (no request for ttlHours) still expires naturally.
        memberId.ifPresent(id -> redisTemplate.expire(key, Duration.ofHours(ttlHours)));
        return memberId;
    }

    private String redisKey(String token) {
        return keyPrefix + token;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
