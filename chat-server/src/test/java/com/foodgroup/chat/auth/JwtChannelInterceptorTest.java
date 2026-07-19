package com.foodgroup.chat.auth;

import com.foodgroup.chat.repository.RoomParticipantLookup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtChannelInterceptorTest {

    @Test
    void acceptsMemberIdPreAuthenticatedByHandshake() {
        JwtVerifier jwtVerifier = mock(JwtVerifier.class);
        RedisMocks redis = redisReturning(null);
        JwtChannelInterceptor interceptor = interceptor(jwtVerifier, redis.template(), null);
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
        JwtChannelInterceptor interceptor = interceptor(jwtVerifier, redis.template(), null);
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
        JwtChannelInterceptor interceptor = interceptor(jwtVerifier, redis.template(), null);
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
    void allowsSubscribeWhenMemberIsRoomParticipant() {
        RoomParticipantLookup lookup = mock(RoomParticipantLookup.class);
        when(lookup.isParticipant("room-9", "member-1")).thenReturn(true);
        JwtChannelInterceptor interceptor = interceptor(mock(JwtVerifier.class), redisReturning(null).template(), lookup);
        Message<byte[]> message = subscribeMessage("room-9", "member-1");

        assertThatCode(() -> interceptor.preSend(message, null)).doesNotThrowAnyException();
    }

    @Test
    void rejectsSubscribeWhenMemberIsNotRoomParticipant() {
        RoomParticipantLookup lookup = mock(RoomParticipantLookup.class);
        when(lookup.isParticipant("room-9", "member-1")).thenReturn(false);
        JwtChannelInterceptor interceptor = interceptor(mock(JwtVerifier.class), redisReturning(null).template(), lookup);
        Message<byte[]> message = subscribeMessage("room-9", "member-1");

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not a room participant");
    }

    @Test
    void rejectsSubscribeWhenNotAuthenticated() {
        JwtChannelInterceptor interceptor = interceptor(mock(JwtVerifier.class), redisReturning(null).template(), mock(RoomParticipantLookup.class));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setDestination("/topic/room/room-9");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not authenticated");
    }

    private Message<byte[]> subscribeMessage(String roomId, String memberId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put(JwtChannelInterceptor.SESSION_MEMBER_ID, memberId);
        accessor.setDestination("/topic/room/" + roomId);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @SuppressWarnings("unchecked")
    private JwtChannelInterceptor interceptor(JwtVerifier jwtVerifier, StringRedisTemplate redisTemplate, RoomParticipantLookup lookup) {
        ObjectProvider<RoomParticipantLookup> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(lookup);
        return new JwtChannelInterceptor(jwtVerifier, redisTemplate, "device-token:", provider);
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
