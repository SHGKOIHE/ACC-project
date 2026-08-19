package com.foodgroup.chat.interceptor;

import com.foodgroup.auth.service.DeviceTokenService;
import com.foodgroup.room.repository.RoomParticipantPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatChannelInterceptor implements ChannelInterceptor {

    private static final String SESSION_MEMBER_ID = "memberId";
    private static final String ROOM_TOPIC_PREFIX = "/topic/room/";
    private static final String BROKER_PREFIX = "/topic/";

    private final DeviceTokenService deviceTokenService;
    private final RoomParticipantPort roomParticipantPort;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("X-Device-Token");
            String memberId = resolveMemberId(token);
            if (memberId == null) {
                throw new IllegalArgumentException("Invalid device token");
            }
            accessor.getSessionAttributes().put(SESSION_MEMBER_ID, memberId);
        }

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            // Clients must never publish directly to a broker-routed destination — chat
            // messages are only ever posted through the room's application endpoint, which
            // enforces membership before broadcasting. A bare SEND here would let any
            // authenticated client push a forged, unchecked payload straight to a room's
            // subscribers.
            if (destination != null && destination.startsWith(BROKER_PREFIX)) {
                throw new IllegalArgumentException("Cannot publish directly to broker destination");
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith(BROKER_PREFIX)) {
                // Deny by default: only an exact "/topic/room/{roomId}" is a valid subscription.
                // Spring's SimpleBroker matches subscriptions with AntPathMatcher, so a bare
                // startsWith("/topic/room/") check would let "/topic/**" (or "/topic/room/*")
                // register unchecked while still matching every room broadcast.
                String memberId = (String) accessor.getSessionAttributes().get(SESSION_MEMBER_ID);
                if (memberId == null) throw new IllegalArgumentException("Not authenticated");
                String roomId = validRoomId(destination);
                if (roomId == null || !roomParticipantPort.existsByRoomIdAndMemberId(roomId, memberId)) {
                    throw new IllegalArgumentException("Not a room participant");
                }
            }
        }

        return message;
    }

    private String resolveMemberId(String token) {
        if (token == null || token.isBlank()) return null;
        // Resolves through Redis (same source of truth as HTTP auth) on every CONNECT instead of
        // an unbounded local cache, so TTL expiry and withdrawal take effect immediately.
        return deviceTokenService.resolveMemberId(token).orElse(null);
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
}
