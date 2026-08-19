package com.foodgroup.chat.controller;

import com.foodgroup.chat.auth.JwtChannelInterceptor;
import com.foodgroup.chat.dto.ChatMessageRequest;
import com.foodgroup.chat.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;

    @MessageMapping("/room/{roomId}/chat")
    public void handleChat(
            @DestinationVariable String roomId,
            @Valid @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        chatMessageService.saveAndPublish(roomId, resolveMemberId(headerAccessor), request.type(), request.content());
    }

    private String resolveMemberId(SimpMessageHeaderAccessor headerAccessor) {
        Object memberId = headerAccessor.getSessionAttributes().get(JwtChannelInterceptor.SESSION_MEMBER_ID);
        return memberId == null ? null : String.valueOf(memberId);
    }
}
