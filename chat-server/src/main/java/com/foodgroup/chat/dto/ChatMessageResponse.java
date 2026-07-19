package com.foodgroup.chat.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        String id,
        String roomId,
        String memberId,
        String nickname,
        ChatMessageType type,
        String content,
        LocalDateTime createdAt
) {
}
