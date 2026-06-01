package com.foodgroup.chat.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodgroup.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatMessageResponse chatMessage = objectMapper.readValue(payload, ChatMessageResponse.class);
            messagingTemplate.convertAndSend("/topic/room/" + chatMessage.roomId(), chatMessage);
        } catch (Exception e) {
            log.warn("Failed to relay Redis chat message", e);
        }
    }
}
