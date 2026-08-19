package com.foodgroup.chat.controller;

import com.foodgroup.chat.auth.JwtChannelInterceptor;
import com.foodgroup.chat.dto.ChatMessageRequest;
import com.foodgroup.chat.dto.ChatMessageType;
import com.foodgroup.chat.service.ChatMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChatControllerTest {

    @Test
    void delegatesIncomingRoomChatMessageToChatMessageService() {
        ChatMessageService chatMessageService = mock(ChatMessageService.class);
        ChatController controller = new ChatController(chatMessageService);
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setSessionAttributes(new HashMap<>());
        headers.getSessionAttributes().put(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1");

        controller.handleChat(
                "room-1",
                new ChatMessageRequest(ChatMessageType.TALK, "hello"),
                headers
        );

        verify(chatMessageService).saveAndPublish("room-1", "member-1", ChatMessageType.TALK, "hello");
    }
}
