package com.foodgroup.chat.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LightsailIpHandshakeInterceptor implements HandshakeInterceptor {

    private static final Set<String> DEFAULT_ALLOWED_SOURCE_IPS = Set.of("43.201.33.167", "100.64.11.88");
    private static final String DEVICE_TOKEN_HEADER = "X-Device-Token";
    private static final String SESSION_DEVICE_TOKEN = "deviceToken";

    private final Set<String> allowedSourceIps;
    private final StringRedisTemplate redisTemplate;
    private final String deviceTokenKeyPrefix;

    public LightsailIpHandshakeInterceptor(
            StringRedisTemplate redisTemplate,
            @Value("${chat.allowed-source-ips:${CHAT_ALLOWED_SOURCE_IPS:${chat.allowed-source-ip:${CHAT_ALLOWED_SOURCE_IP:43.201.33.167,100.64.11.88}}}}") String allowedSourceIps,
            @Value("${chat.auth.device-token-key-prefix:${CHAT_DEVICE_TOKEN_KEY_PREFIX:device-token:}}") String deviceTokenKeyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.allowedSourceIps = parseAllowedSourceIps(allowedSourceIps);
        this.deviceTokenKeyPrefix = deviceTokenKeyPrefix == null ? "" : deviceTokenKeyPrefix;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String sourceIp = resolveSourceIp(request);
        if (!allowedSourceIps.contains(sourceIp)) {
            log.warn("Rejected WebSocket handshake from {}", sourceIp);
            return false;
        }

        String deviceToken = request.getHeaders().getFirst(DEVICE_TOKEN_HEADER);
        if (deviceToken == null || deviceToken.isBlank()) {
            return true;
        }

        String memberId = resolveMemberId(deviceToken.trim());
        if (memberId == null || memberId.isBlank()) {
            log.warn("Rejected WebSocket handshake from {}: invalid device token", sourceIp);
            return false;
        }

        attributes.put(JwtChannelInterceptor.SESSION_MEMBER_ID, memberId);
        attributes.put(SESSION_DEVICE_TOKEN, deviceToken.trim());
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private String resolveMemberId(String deviceToken) {
        try {
            return redisTemplate.opsForValue().get(deviceTokenKeyPrefix + deviceToken);
        } catch (Exception e) {
            log.warn("Failed to validate device token from Redis", e);
            return null;
        }
    }

    private static Set<String> parseAllowedSourceIps(String value) {
        Set<String> ips = new LinkedHashSet<>(DEFAULT_ALLOWED_SOURCE_IPS);
        ips.addAll(Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isBlank())
                .collect(Collectors.toSet()));
        return Set.copyOf(ips);
    }

    private String resolveSourceIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress == null || remoteAddress.getAddress() == null
                ? ""
                : remoteAddress.getAddress().getHostAddress();
    }
}
