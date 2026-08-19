package com.foodgroup.chat.repository;

import com.foodgroup.chat.domain.DynamoChatMessage;
import com.foodgroup.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

// Writes into the same "ChatMessages" table backend's DynamoChatMessageAdapter reads from
// (backend/src/main/java/com/foodgroup/chat/repository/DynamoChatMessageAdapter.java), using
// the identical roomId/createdAtId key scheme so REST history reads see what STOMP clients sent.
@Component
@RequiredArgsConstructor
public class ChatMessageWriter {

    private static final String TABLE_NAME = "ChatMessages";
    private static final long TTL_DAYS = 30;

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<DynamoChatMessage> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoChatMessage.class));
    }

    public void save(ChatMessageResponse message) {
        LocalDateTime now = LocalDateTime.now();

        DynamoChatMessage item = new DynamoChatMessage();
        item.setRoomId(message.roomId());
        item.setCreatedAtId(now.toString() + "#" + message.id());
        item.setMemberId(message.memberId());
        item.setType(message.type().name());
        item.setContent(message.content());
        item.setExpiresAt(now.plusDays(TTL_DAYS).toInstant(ZoneOffset.UTC).getEpochSecond());

        table().putItem(item);
    }
}
