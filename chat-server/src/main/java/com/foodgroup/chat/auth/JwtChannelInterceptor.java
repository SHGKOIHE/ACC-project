package com.foodgroup.chat.auth;

import com.foodgroup.chat.repository.RoomParticipantChecker;
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
    private static final String ROOM_TOPIC_PREFIX = "/topic/room/";
    private static final String BROKER_PREFIX = "/topic/";

    private final JwtVerifier jwtVerifier;
    private final StringRedisTemplate redisTemplate;
    private final String deviceTokenKeyPrefix;
    private final RoomParticipantChecker roomParticipantChecker;

    public JwtChannelInterceptor(
            JwtVerifier jwtVerifier,
            StringRedisTemplate redisTemplate,
            @Value("${chat.auth.device-token-key-prefix:${CHAT_DEVICE_TOKEN_KEY_PREFIX:device-token:}}") String deviceTokenKeyPrefix,
            RoomParticipantChecker roomParticipantChecker
    ) {
        this.jwtVerifier = jwtVerifier;
        this.redisTemplate = redisTemplate;
        this.deviceTokenKeyPrefix = deviceTokenKeyPrefix == null ? "" : deviceTokenKeyPrefix;
        this.roomParticipantChecker = roomParticipantChecker;
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

            String destination = accessor.getDestination();

            if (StompCommand.SEND.equals(accessor.getCommand())) {
                // Clients must never publish directly to a broker-routed destination — chat
                // messages only ever flow through the /app-prefixed ChatController, which
                // enforces room membership before persisting/broadcasting. Allowing a bare
                // SEND here would let any authenticated client push a forged, unchecked
                // payload straight to a room's subscribers.
                if (destination != null && destination.startsWith(BROKER_PREFIX)) {
                    throw new IllegalArgumentException("Cannot publish directly to broker destination");
                }
            }

            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) && destination != null && destination.startsWith(BROKER_PREFIX)) {
                // Deny by default: only an exact "/topic/room/{roomId}" is a valid subscription.
                // Spring's SimpleBroker matches subscriptions with AntPathMatcher, so a bare
                // startsWith(ROOM_TOPIC_PREFIX) check would let "/topic/**" (or "/topic/room/*")
                // register unchecked while still matching every room broadcast.
                String memberId = String.valueOf(accessor.getSessionAttributes().get(SESSION_MEMBER_ID));
                String roomId = validRoomId(destination);
                if (roomId == null || !roomParticipantChecker.isParticipant(roomId, memberId)) {
                    throw new IllegalArgumentException("Not a room participant");
                }
            }
        }

        return message;
    }

    private static String validRoomId(String destination) {
        if (!destination.startsWith(ROOM_TOPIC_PREFIX)) {
            return null;
        }
        String roomId = destination.substring(ROOM_TOPIC_PREFIX.length());
        if (roomId.isBlank() || roomId.indexOf('/') >= 0 || roomId.indexOf('*') >= 0 || roomId.indexOf('#') >= 0) {
            return null;
        }
        return roomId;
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
