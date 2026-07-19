package com.foodgroup.chat.auth;

import com.foodgroup.chat.repository.RoomParticipantLookup;
import org.springframework.beans.factory.ObjectProvider;
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
    private static final String ROOM_PATH_MARKER = "/room/";

    private final JwtVerifier jwtVerifier;
    private final StringRedisTemplate redisTemplate;
    private final String deviceTokenKeyPrefix;
    // DynamoDB가 비활성화된 로컬 환경에서는 참여자 검증을 건너뛰도록 Optional 주입
    private final ObjectProvider<RoomParticipantLookup> roomParticipantLookupProvider;

    public JwtChannelInterceptor(
            JwtVerifier jwtVerifier,
            StringRedisTemplate redisTemplate,
            @Value("${chat.auth.device-token-key-prefix:${CHAT_DEVICE_TOKEN_KEY_PREFIX:device-token:}}") String deviceTokenKeyPrefix,
            ObjectProvider<RoomParticipantLookup> roomParticipantLookupProvider
    ) {
        this.jwtVerifier = jwtVerifier;
        this.redisTemplate = redisTemplate;
        this.deviceTokenKeyPrefix = deviceTokenKeyPrefix == null ? "" : deviceTokenKeyPrefix;
        this.roomParticipantLookupProvider = roomParticipantLookupProvider;
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
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            Object memberId = sessionAttributes == null ? null : sessionAttributes.get(SESSION_MEMBER_ID);
            if (memberId == null) {
                throw new IllegalArgumentException("Not authenticated");
            }

            // BR-CHAT-01: 방 참여자만 해당 방의 채팅을 구독/전송할 수 있다.
            String roomId = extractRoomId(accessor.getDestination());
            if (roomId != null) {
                RoomParticipantLookup lookup = roomParticipantLookupProvider.getIfAvailable();
                if (lookup != null && !lookup.isParticipant(roomId, String.valueOf(memberId))) {
                    throw new IllegalArgumentException("Not a room participant");
                }
            }
        }

        return message;
    }

    /**
     * "/topic/room/{roomId}" 및 "/app/room/{roomId}/chat" 형태에서 roomId를 추출한다.
     * 방 관련 목적지가 아니면 null.
     */
    private String extractRoomId(String destination) {
        if (destination == null) {
            return null;
        }
        int idx = destination.indexOf(ROOM_PATH_MARKER);
        if (idx < 0) {
            return null;
        }
        String rest = destination.substring(idx + ROOM_PATH_MARKER.length());
        int slash = rest.indexOf('/');
        String roomId = slash < 0 ? rest : rest.substring(0, slash);
        return roomId.isBlank() ? null : roomId;
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
