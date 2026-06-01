package com.foodgroup.chat.interceptor;

import com.foodgroup.auth.repository.MemberPort;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatChannelInterceptor implements ChannelInterceptor {

    private static final String SESSION_MEMBER_ID = "memberId";

    private final MemberPort memberPort;
    private final RoomParticipantPort roomParticipantPort;
    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();

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

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/room/")) {
                String memberId = (String) accessor.getSessionAttributes().get(SESSION_MEMBER_ID);
                if (memberId == null) throw new IllegalArgumentException("Not authenticated");
                String roomId = extractRoomId(destination);
                if (roomId != null && !roomParticipantPort.existsByRoomIdAndMemberId(roomId, memberId)) {
                    throw new IllegalArgumentException("Not a room participant");
                }
            }
        }

        return message;
    }

    private String resolveMemberId(String token) {
        if (token == null || token.isBlank()) return null;
        return tokenCache.computeIfAbsent(token,
                t -> memberPort.findByDeviceToken(t).map(m -> m.getId()).orElse(null));
    }

    private String extractRoomId(String destination) {
        try {
            String[] parts = destination.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            return null;
        }
    }
}
