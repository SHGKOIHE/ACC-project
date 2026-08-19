package com.foodgroup.chat.auth;

import com.foodgroup.chat.repository.RoomParticipantChecker;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtChannelInterceptorTest {

    @Test
    void acceptsMemberIdPreAuthenticatedByHandshake() {
        JwtVerifier jwtVerifier = mock(JwtVerifier.class);
        RedisMocks redis = redisReturning(null);
        JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtVerifier, redis.template(), "device-token:", mock(RoomParticipantChecker.class));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, null);

        StompHeaderAccessor updated = StompHeaderAccessor.wrap(message);
        assertThat(updated.getUser()).isNotNull();
        assertThat(updated.getUser().getName()).isEqualTo("member-1");
        verifyNoInteractions(jwtVerifier);
    }

    @Test
    void acceptsDeviceTokenFromStompConnectHeaders() {
        JwtVerifier jwtVerifier = mock(JwtVerifier.class);
        RedisMocks redis = redisReturning("member-2");
        JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtVerifier, redis.template(), "device-token:", mock(RoomParticipantChecker.class));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("X-Device-Token", "device-2");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, null);

        StompHeaderAccessor updated = StompHeaderAccessor.wrap(message);
        assertThat(updated.getUser()).isNotNull();
        assertThat(updated.getUser().getName()).isEqualTo("member-2");
        assertThat(updated.getSessionAttributes()).containsEntry(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-2");
        verifyNoInteractions(jwtVerifier);
    }

    @Test
    void rejectsUnknownDeviceTokenFromStompConnectHeaders() {
        JwtVerifier jwtVerifier = mock(JwtVerifier.class);
        RedisMocks redis = redisReturning(null);
        JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtVerifier, redis.template(), "device-token:", mock(RoomParticipantChecker.class));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("X-Device-Token", "unknown-device");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid device token");

        verifyNoInteractions(jwtVerifier);
    }

    @Test
    void allowsSubscribeToRoomTopicWhenCallerIsParticipant() {
        JwtVerifier jwtVerifier = mock(JwtVerifier.class);
        RoomParticipantChecker checker = mock(RoomParticipantChecker.class);
        when(checker.isParticipant("room-1", "member-1")).thenReturn(true);
        JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtVerifier, mock(StringRedisTemplate.class), "device-token:", checker);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/room/room-1");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNotNull();
    }

    @Test
    void rejectsSubscribeToRoomTopicWhenCallerIsNotParticipant() {
        JwtVerifier jwtVerifier = mock(JwtVerifier.class);
        RoomParticipantChecker checker = mock(RoomParticipantChecker.class);
        when(checker.isParticipant("room-1", "member-1")).thenReturn(false);
        JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtVerifier, mock(StringRedisTemplate.class), "device-token:", checker);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/room/room-1");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not a room participant");
    }

    @Test
    void rejectsWildcardSubscriptionEvenWithoutCheckingParticipation() {
        JwtVerifier jwtVerifier = mock(JwtVerifier.class);
        RoomParticipantChecker checker = mock(RoomParticipantChecker.class);
        JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtVerifier, mock(StringRedisTemplate.class), "device-token:", checker);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/**");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not a room participant");

        verifyNoInteractions(checker);
    }

    @Test
    void rejectsSubscriptionToRoomIdContainingExtraPathSegments() {
        JwtVerifier jwtVerifier = mock(JwtVerifier.class);
        RoomParticipantChecker checker = mock(RoomParticipantChecker.class);
        JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtVerifier, mock(StringRedisTemplate.class), "device-token:", checker);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/room/room-1/../room-2");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not a room participant");

        verifyNoInteractions(checker);
    }

    @Test
    void rejectsDirectSendToBrokerDestination() {
        JwtVerifier jwtVerifier = mock(JwtVerifier.class);
        RoomParticipantChecker checker = mock(RoomParticipantChecker.class);
        JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtVerifier, mock(StringRedisTemplate.class), "device-token:", checker);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/topic/room/room-1");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot publish directly to broker destination");

        verifyNoInteractions(checker);
    }

    @Test
    void allowsSendToApplicationDestination() {
        JwtVerifier jwtVerifier = mock(JwtVerifier.class);
        RoomParticipantChecker checker = mock(RoomParticipantChecker.class);
        JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtVerifier, mock(StringRedisTemplate.class), "device-token:", checker);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/room/room-1/chat");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNotNull();
        verifyNoInteractions(checker);
    }

    @SuppressWarnings("unchecked")
    private RedisMocks redisReturning(String memberId) {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get(org.mockito.ArgumentMatchers.anyString())).thenReturn(memberId);
        return new RedisMocks(redisTemplate, values);
    }

    private record RedisMocks(StringRedisTemplate template, ValueOperations<String, String> values) {
    }
}
