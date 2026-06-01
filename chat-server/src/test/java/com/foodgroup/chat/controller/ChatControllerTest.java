package com.foodgroup.chat.controller;

import com.foodgroup.chat.auth.JwtChannelInterceptor;
import com.foodgroup.chat.dto.ChatMessageRequest;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.dto.ChatMessageType;
import com.foodgroup.chat.pubsub.RedisChatPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChatControllerTest {

    @Test
    void publishesIncomingRoomChatMessageToRedis() {
        RedisChatPublisher publisher = mock(RedisChatPublisher.class);
        ChatController controller = new ChatController(publisher);
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setSessionAttributes(new HashMap<>());
        headers.getSessionAttributes().put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1");

        controller.handleChat(
                "room-1",
                new ChatMessageRequest(ChatMessageType.TALK, "hello"),
                headers
        );

        ArgumentCaptor<ChatMessageResponse> captor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(publisher).publish(captor.capture());
        ChatMessageResponse message = captor.getValue();
        assertThat(message.id()).isNotBlank();
        assertThat(message.roomId()).isEqualTo("room-1");
        assertThat(message.memberId()).isEqualTo("member-1");
        assertThat(message.type()).isEqualTo(ChatMessageType.TALK);
        assertThat(message.content()).isEqualTo("hello");
        assertThat(message.createdAt()).isNotNull();
    }
}
