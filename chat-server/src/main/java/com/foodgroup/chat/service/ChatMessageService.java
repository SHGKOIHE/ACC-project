package com.foodgroup.chat.service;

import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.dto.ChatMessageType;
import com.foodgroup.chat.pubsub.RedisChatPublisher;
import com.foodgroup.chat.repository.ChatMessageWriter;
import com.foodgroup.chat.repository.RoomParticipantChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final RoomParticipantChecker roomParticipantChecker;
    private final ChatMessageWriter chatMessageWriter;
    private final RedisChatPublisher redisChatPublisher;

    public ChatMessageResponse saveAndPublish(String roomId, String memberId, ChatMessageType type, String content) {
        if (memberId == null || !roomParticipantChecker.isParticipant(roomId, memberId)) {
            throw new IllegalArgumentException("Not a room participant");
        }

        ChatMessageResponse message = new ChatMessageResponse(
                UUID.randomUUID().toString(),
                roomId,
                memberId,
                null,
                type,
                content,
                Instant.now()
        );

        chatMessageWriter.save(message);
        redisChatPublisher.publish(message);
        return message;
    }
}
