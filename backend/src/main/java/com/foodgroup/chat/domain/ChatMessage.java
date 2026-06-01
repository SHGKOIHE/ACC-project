package com.foodgroup.chat.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ChatMessage {
    private String id;
    private String roomId;
    private String memberId;
    private ChatMessageType type;
    private String content;
    private LocalDateTime createdAt;
}
