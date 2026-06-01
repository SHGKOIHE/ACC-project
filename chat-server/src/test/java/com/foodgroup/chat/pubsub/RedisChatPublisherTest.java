package com.foodgroup.chat.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.dto.ChatMessageType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisChatPublisherTest {

    @Test
    void serializesMessageAndPublishesToRoomChannel() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RedisChatPublisher publisher = new RedisChatPublisher(redisTemplate, objectMapper);
        ChatMessageResponse message = new ChatMessageResponse(
                "msg-1",
                "room-1",
                "member-1",
                null,
                ChatMessageType.TALK,
                "hello",
                Instant.parse("2026-06-01T00:00:00Z")
        );

        publisher.publish(message);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq("chat.room.room-1"), payloadCaptor.capture());
        ChatMessageResponse decoded = objectMapper.readValue(payloadCaptor.getValue(), ChatMessageResponse.class);
        assertThat(decoded).isEqualTo(message);
    }
}
