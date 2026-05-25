package com.foodgroup.chat.dto;

import com.foodgroup.chat.domain.ChatMessage;
import com.foodgroup.chat.domain.ChatMessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long roomId,
        Long memberId,
        String nickname,
        ChatMessageType type,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse of(ChatMessage msg, String nickname) {
        return new ChatMessageResponse(
                msg.getId(), msg.getRoomId(), msg.getMemberId(),
                nickname, msg.getType(), msg.getContent(), msg.getCreatedAt()
        );
    }
}
