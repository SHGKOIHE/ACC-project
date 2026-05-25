package com.foodgroup.chat.interceptor;

import com.foodgroup.auth.repository.MemberRepository;
import com.foodgroup.room.repository.RoomParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatChannelInterceptor implements ChannelInterceptor {

    private static final String CACHE_PREFIX = "auth:device:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String SESSION_MEMBER_ID = "memberId";

    private final MemberRepository memberRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("X-Device-Token");
            Long memberId = resolveMemberId(token);
            if (memberId == null) {
                throw new IllegalArgumentException("Invalid device token");
            }
            accessor.getSessionAttributes().put(SESSION_MEMBER_ID, memberId);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/room/")) {
                Long memberId = (Long) accessor.getSessionAttributes().get(SESSION_MEMBER_ID);
                if (memberId == null) throw new IllegalArgumentException("Not authenticated");
                Long roomId = extractRoomId(destination);
                if (roomId != null && memberId != null
                        && !roomParticipantRepository.existsByRoomIdAndMemberId(roomId, memberId)) {
                    throw new IllegalArgumentException("Not a room participant");
                }
            }
        }

        return message;
    }

    private Long resolveMemberId(String token) {
        if (token == null || token.isBlank()) return null;
        String cacheKey = CACHE_PREFIX + token;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return Long.parseLong(cached);

        return memberRepository.findByDeviceToken(token).map(m -> {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(m.getId()), CACHE_TTL);
            return m.getId();
        }).orElse(null);
    }

    private Long extractRoomId(String destination) {
        try {
            String[] parts = destination.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            return null;
        }
    }
}
