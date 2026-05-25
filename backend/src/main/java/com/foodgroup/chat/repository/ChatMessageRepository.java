package com.foodgroup.chat.repository;

import com.foodgroup.chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query(value = "SELECT * FROM chat_messages WHERE room_id = :roomId ORDER BY created_at DESC LIMIT 50",
           nativeQuery = true)
    List<ChatMessage> findTop50ByRoomIdOrderByCreatedAtDesc(@Param("roomId") Long roomId);

    @Modifying
    @Query(value = "DELETE FROM chat_messages WHERE room_id IN " +
                   "(SELECT id FROM rooms WHERE status IN ('COMPLETED', 'CANCELLED') AND updated_at < :cutoff)",
           nativeQuery = true)
    void deleteOldMessages(@Param("cutoff") LocalDateTime cutoff);
}
