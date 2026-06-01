package com.foodgroup.chat.repository;

import com.foodgroup.chat.domain.ChatMessage;
import com.foodgroup.chat.domain.ChatMessageType;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessagePort {
    ChatMessage save(String roomId, String memberId, ChatMessageType type, String content);
    List<ChatMessage> findTop50ByRoomId(String roomId);
    void deleteOldMessages(LocalDateTime cutoff);
}
