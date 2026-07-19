package com.foodgroup.chat.event;

import com.foodgroup.chat.auth.JwtChannelInterceptor;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.dto.ChatMessageType;
import com.foodgroup.chat.pubsub.RedisChatPublisher;
import com.foodgroup.chat.repository.ChatMessageStore;
import com.foodgroup.chat.repository.MemberNicknameLookup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatSessionEventListenerTest {

    @Test
    void publishesEnterMessageWhenSubscribingToRoomTopic() {
        RedisChatPublisher publisher = mock(RedisChatPublisher.class);
        ChatSessionEventListener listener = listener(publisher);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1");
        listener.handleSubscribe(new SessionSubscribeEvent(this,
                stompMessage(StompCommand.SUBSCRIBE, "/topic/room/room-1", attrs)));

        ChatMessageResponse msg = capturePublished(publisher);
        assertThat(msg.type()).isEqualTo(ChatMessageType.ENTER);
        assertThat(msg.roomId()).isEqualTo("room-1");
        assertThat(msg.memberId()).isEqualTo("member-1");
        assertThat(msg.content()).contains("입장");
        // 퇴장 시점에 쓰기 위해 roomId가 세션에 기억되어야 한다
        assertThat(attrs).containsEntry("roomId", "room-1");
    }

    @Test
    void ignoresSubscribeToNonRoomDestination() {
        RedisChatPublisher publisher = mock(RedisChatPublisher.class);
        ChatSessionEventListener listener = listener(publisher);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1");
        listener.handleSubscribe(new SessionSubscribeEvent(this,
                stompMessage(StompCommand.SUBSCRIBE, "/topic/notifications", attrs)));

        verifyNoInteractions(publisher);
    }

    @Test
    void publishesLeaveMessageOnDisconnect() {
        RedisChatPublisher publisher = mock(RedisChatPublisher.class);
        ChatSessionEventListener listener = listener(publisher);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1");
        attrs.put("roomId", "room-1");
        attrs.put("nickname", "홍길동");
        SessionDisconnectEvent event = new SessionDisconnectEvent(this,
                stompMessage(StompCommand.DISCONNECT, null, attrs), "sess-1", CloseStatus.NORMAL);
        listener.handleDisconnect(event);

        ChatMessageResponse msg = capturePublished(publisher);
        assertThat(msg.type()).isEqualTo(ChatMessageType.LEAVE);
        assertThat(msg.roomId()).isEqualTo("room-1");
        assertThat(msg.content()).isEqualTo("홍길동님이 나갔습니다");
    }

    @Test
    void ignoresDisconnectWhenSessionNeverJoinedRoom() {
        RedisChatPublisher publisher = mock(RedisChatPublisher.class);
        ChatSessionEventListener listener = listener(publisher);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1"); // roomId 없음
        listener.handleDisconnect(new SessionDisconnectEvent(this,
                stompMessage(StompCommand.DISCONNECT, null, attrs), "sess-1", CloseStatus.NORMAL));

        verifyNoInteractions(publisher);
    }

    @SuppressWarnings("unchecked")
    private ChatSessionEventListener listener(RedisChatPublisher publisher) {
        ObjectProvider<ChatMessageStore> storeProvider = mock(ObjectProvider.class);
        when(storeProvider.getIfAvailable()).thenReturn(null);
        ObjectProvider<MemberNicknameLookup> nickProvider = mock(ObjectProvider.class);
        when(nickProvider.getIfAvailable()).thenReturn(null);
        return new ChatSessionEventListener(publisher, storeProvider, nickProvider);
    }

    private Message<byte[]> stompMessage(StompCommand command, String destination, Map<String, Object> attrs) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionAttributes(attrs);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private ChatMessageResponse capturePublished(RedisChatPublisher publisher) {
        ArgumentCaptor<ChatMessageResponse> captor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(publisher).publish(captor.capture());
        return captor.getValue();
    }
}
