package com.foodgroup.chat.controller;

import com.foodgroup.chat.auth.JwtChannelInterceptor;
import com.foodgroup.chat.dto.ChatMessageRequest;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.pubsub.RedisChatPublisher;
import com.foodgroup.chat.repository.ChatMessageStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final RedisChatPublisher redisChatPublisher;
    // DynamoDB가 비활성화된 로컬 환경에서는 저장 없이 발행만 하도록 Optional 주입
    private final ObjectProvider<ChatMessageStore> chatMessageStoreProvider;

    @MessageMapping("/room/{roomId}/chat")
    public void handleChat(
            @DestinationVariable String roomId,
            @Valid @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String memberId = resolveMemberId(headerAccessor);

        ChatMessageStore store = chatMessageStoreProvider.getIfAvailable();
        ChatMessageResponse message = (store != null)
                ? store.save(roomId, memberId, null, request.type(), request.content())
                : new ChatMessageResponse(
                        UUID.randomUUID().toString(), roomId, memberId, null,
                        request.type(), request.content(), LocalDateTime.now());

        redisChatPublisher.publish(message);
    }

    private String resolveMemberId(SimpMessageHeaderAccessor headerAccessor) {
        Object memberId = headerAccessor.getSessionAttributes().get(JwtChannelInterceptor.SESSION_MEMBER_ID);
        return memberId == null ? null : String.valueOf(memberId);
    }
}
