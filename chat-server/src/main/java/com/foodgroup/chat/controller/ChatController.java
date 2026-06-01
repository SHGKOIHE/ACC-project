package com.foodgroup.chat.controller;

import com.foodgroup.chat.auth.JwtChannelInterceptor;
import com.foodgroup.chat.dto.ChatMessageRequest;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.pubsub.RedisChatPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final RedisChatPublisher redisChatPublisher;

    @MessageMapping("/room/{roomId}/chat")
    public void handleChat(
            @DestinationVariable String roomId,
            @Valid @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        ChatMessageResponse message = new ChatMessageResponse(
                UUID.randomUUID().toString(),
                roomId,
                resolveMemberId(headerAccessor),
                null,
                request.type(),
                request.content(),
                Instant.now()
        );
        redisChatPublisher.publish(message);
    }

    private String resolveMemberId(SimpMessageHeaderAccessor headerAccessor) {
        Object memberId = headerAccessor.getSessionAttributes().get(JwtChannelInterceptor.SESSION_MEMBER_ID);
        return memberId == null ? null : String.valueOf(memberId);
    }
}
