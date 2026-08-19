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
import java.util.Map;

@Slf4j
@Component
public class LightsailIpHandshakeInterceptor implements HandshakeInterceptor {

    private static final String DEVICE_TOKEN_HEADER = "X-Device-Token";
    private static final String SESSION_DEVICE_TOKEN = "deviceToken";

    private final StringRedisTemplate redisTemplate;
    private final String deviceTokenKeyPrefix;

    public LightsailIpHandshakeInterceptor(
            StringRedisTemplate redisTemplate,
            @Value("${chat.auth.device-token-key-prefix:${CHAT_DEVICE_TOKEN_KEY_PREFIX:device-token:}}") String deviceTokenKeyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.deviceTokenKeyPrefix = deviceTokenKeyPrefix == null ? "" : deviceTokenKeyPrefix;
    }

    // Standard WebSocket clients (browsers, React Native's WebSocket) cannot set custom HTTP
    // headers on the handshake request, so X-Device-Token is normally absent here — real
    // authentication happens at STOMP CONNECT via JwtChannelInterceptor, which reads the token
    // from the CONNECT frame instead. This interceptor only opportunistically pre-authenticates
    // clients that CAN send the header, and never blocks ones that can't.
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String sourceIp = resolveSourceIp(request);

        String deviceToken = request.getHeaders().getFirst(DEVICE_TOKEN_HEADER);
        if (deviceToken != null && !deviceToken.isBlank()) {
            String memberId = resolveMemberId(deviceToken.trim());
            if (memberId != null && !memberId.isBlank()) {
                attributes.put(JwtChannelInterceptor.SESSION_MEMBER_ID, memberId);
                attributes.put(SESSION_DEVICE_TOKEN, deviceToken.trim());
            }
        }

        log.info("WebSocket handshake accepted from {}", sourceIp);
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
