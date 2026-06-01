package com.foodgroup.chat.dto;

import java.time.Instant;

public record ChatMessageResponse(
        String id,
        String roomId,
        String memberId,
        String nickname,
        ChatMessageType type,
        String content,
        Instant createdAt
) {
}
