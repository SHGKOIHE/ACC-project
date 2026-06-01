package com.foodgroup.chat.controller;

import com.foodgroup.chat.dto.ChatMessageRequest;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/room/{roomId}/chat")
    public void handleChat(@DestinationVariable String roomId,
                           ChatMessageRequest request,
                           SimpMessageHeaderAccessor headerAccessor) {
        String memberId = (String) headerAccessor.getSessionAttributes().get("memberId");
        chatService.saveAndBroadcast(roomId, memberId, request.type(), request.content());
    }
}
