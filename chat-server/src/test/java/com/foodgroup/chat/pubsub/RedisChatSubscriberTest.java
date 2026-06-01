package com.foodgroup.chat.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.dto.ChatMessageType;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisChatSubscriberTest {

    @Test
    void relaysRedisRoomMessageToWebSocketTopic() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisChatSubscriber subscriber = new RedisChatSubscriber(objectMapper, messagingTemplate);
        ChatMessageResponse message = new ChatMessageResponse(
                "msg-1",
                "room-1",
                "member-1",
                null,
                ChatMessageType.TALK,
                "hello",
                Instant.parse("2026-06-01T00:00:00Z")
        );
        Message redisMessage = mock(Message.class);
        org.mockito.Mockito.when(redisMessage.getBody()).thenReturn(objectMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8));

        subscriber.onMessage(redisMessage, null);

        verify(messagingTemplate).convertAndSend("/topic/room/room-1", message);
    }
}
