package com.foodgroup.chat.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    public static final String SESSION_MEMBER_ID = "memberId";
    private static final String DEVICE_TOKEN_HEADER = "X-Device-Token";

    private final JwtVerifier jwtVerifier;
    private final StringRedisTemplate redisTemplate;
    private final String deviceTokenKeyPrefix;

    public JwtChannelInterceptor(
            JwtVerifier jwtVerifier,
            StringRedisTemplate redisTemplate,
            @Value("${chat.auth.device-token-key-prefix:${CHAT_DEVICE_TOKEN_KEY_PREFIX:device-token:}}") String deviceTokenKeyPrefix
    ) {
        this.jwtVerifier = jwtVerifier;
        this.redisTemplate = redisTemplate;
        this.deviceTokenKeyPrefix = deviceTokenKeyPrefix == null ? "" : deviceTokenKeyPrefix;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = sessionAttributes(accessor);
            Object preAuthenticatedMemberId = sessionAttributes.get(SESSION_MEMBER_ID);
            if (preAuthenticatedMemberId != null && !String.valueOf(preAuthenticatedMemberId).isBlank()) {
                accessor.setUser(new StompPrincipal(String.valueOf(preAuthenticatedMemberId)));
                return message;
            }

            String deviceToken = accessor.getFirstNativeHeader(DEVICE_TOKEN_HEADER);
            if (deviceToken != null && !deviceToken.isBlank()) {
                String memberId = resolveMemberIdByDeviceToken(deviceToken.trim());
                if (memberId == null || memberId.isBlank()) {
                    throw new IllegalArgumentException("Invalid device token");
                }
                sessionAttributes.put(SESSION_MEMBER_ID, memberId);
                accessor.setUser(new StompPrincipal(memberId));
                return message;
            }

            JwtClaims claims = jwtVerifier.verify(resolveToken(accessor));
            String memberId = claims.effectiveMemberId();
            sessionAttributes.put(SESSION_MEMBER_ID, memberId);
            accessor.setUser(new StompPrincipal(memberId));
            return message;
        }

        if (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getSessionAttributes() == null || accessor.getSessionAttributes().get(SESSION_MEMBER_ID) == null) {
                throw new IllegalArgumentException("Not authenticated");
            }
        }

        return message;
    }

    private Map<String, Object> sessionAttributes(StompHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
            accessor.setSessionAttributes(attributes);
        }
        return attributes;
    }

    private String resolveMemberIdByDeviceToken(String deviceToken) {
        return redisTemplate.opsForValue().get(deviceTokenKeyPrefix + deviceToken);
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            return authorization;
        }
        String accessToken = accessor.getFirstNativeHeader("access_token");
        if (accessToken != null && !accessToken.isBlank()) {
            return accessToken;
        }
        return accessor.getFirstNativeHeader("token");
    }
}
