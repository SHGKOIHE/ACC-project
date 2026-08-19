package com.foodgroup.chat.interceptor;

import com.foodgroup.auth.service.DeviceTokenService;
import com.foodgroup.room.repository.RoomParticipantPort;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatChannelInterceptorTest {

    @Test
    void resolvesMemberIdThroughDeviceTokenServiceOnEveryConnect() {
        DeviceTokenService deviceTokenService = mock(DeviceTokenService.class);
        when(deviceTokenService.resolveMemberId("tok-1")).thenReturn(Optional.of("member-1"));
        ChatChannelInterceptor interceptor = new ChatChannelInterceptor(deviceTokenService, mock(RoomParticipantPort.class));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("X-Device-Token", "tok-1");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, null);

        StompHeaderAccessor updated = StompHeaderAccessor.wrap(message);
        assertThat(updated.getSessionAttributes()).containsEntry("memberId", "member-1");
    }

    @Test
    void rejectsConnectWhenDeviceTokenServiceCannotResolveToken() {
        DeviceTokenService deviceTokenService = mock(DeviceTokenService.class);
        when(deviceTokenService.resolveMemberId("expired-tok")).thenReturn(Optional.empty());
        ChatChannelInterceptor interceptor = new ChatChannelInterceptor(deviceTokenService, mock(RoomParticipantPort.class));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("X-Device-Token", "expired-tok");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid device token");
    }

    @Test
    void rejectsSubscribeToRoomTopicWhenNotAParticipant() {
        DeviceTokenService deviceTokenService = mock(DeviceTokenService.class);
        RoomParticipantPort roomParticipantPort = mock(RoomParticipantPort.class);
        when(roomParticipantPort.existsByRoomIdAndMemberId("room-1", "member-1")).thenReturn(false);
        ChatChannelInterceptor interceptor = new ChatChannelInterceptor(deviceTokenService, roomParticipantPort);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/room/room-1");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put("memberId", "member-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not a room participant");
    }

    @Test
    void rejectsWildcardSubscriptionEvenWithoutCheckingParticipation() {
        DeviceTokenService deviceTokenService = mock(DeviceTokenService.class);
        RoomParticipantPort roomParticipantPort = mock(RoomParticipantPort.class);
        ChatChannelInterceptor interceptor = new ChatChannelInterceptor(deviceTokenService, roomParticipantPort);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/**");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put("memberId", "member-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not a room participant");

        verifyNoInteractions(roomParticipantPort);
    }

    @Test
    void rejectsDirectSendToBrokerDestination() {
        DeviceTokenService deviceTokenService = mock(DeviceTokenService.class);
        RoomParticipantPort roomParticipantPort = mock(RoomParticipantPort.class);
        ChatChannelInterceptor interceptor = new ChatChannelInterceptor(deviceTokenService, roomParticipantPort);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/topic/room/room-1");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put("memberId", "member-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot publish directly to broker destination");

        verifyNoInteractions(roomParticipantPort);
    }
}
