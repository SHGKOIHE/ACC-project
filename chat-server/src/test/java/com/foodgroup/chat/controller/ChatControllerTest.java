package com.foodgroup.chat.controller;

import com.foodgroup.chat.auth.JwtChannelInterceptor;
import com.foodgroup.chat.dto.ChatMessageRequest;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.dto.ChatMessageType;
import com.foodgroup.chat.pubsub.RedisChatPublisher;
import com.foodgroup.chat.repository.ChatMessageStore;
import com.foodgroup.chat.repository.MemberNicknameLookup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void publishesIncomingRoomChatMessageToRedis() {
        RedisChatPublisher publisher = mock(RedisChatPublisher.class);
        ObjectProvider<ChatMessageStore> storeProvider = mock(ObjectProvider.class);
        when(storeProvider.getIfAvailable()).thenReturn(null); // DynamoDB 비활성: 발행만
        ObjectProvider<MemberNicknameLookup> nicknameProvider = mock(ObjectProvider.class);
        when(nicknameProvider.getIfAvailable()).thenReturn(null); // 조회 불가: 기본 닉네임
        ChatController controller = new ChatController(publisher, storeProvider, nicknameProvider);
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
