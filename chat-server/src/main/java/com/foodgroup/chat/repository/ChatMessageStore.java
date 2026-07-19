package com.foodgroup.chat.repository;

import com.foodgroup.chat.domain.DynamoChatMessage;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.dto.ChatMessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ChatMessageStore {

    private static final String TABLE_NAME = "ChatMessages";
    private static final long TTL_DAYS = 30;

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<DynamoChatMessage> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoChatMessage.class));
    }

    /**
     * 메시지를 DynamoDB에 저장하고, 저장된 내용을 그대로 응답 DTO로 반환한다.
     * createdAtId(정렬키)와 createdAt 형식은 backend 이력 조회와 동일하게 맞춘다.
     */
    public ChatMessageResponse save(String roomId, String memberId, String nickname,
                                    ChatMessageType type, String content) {
        LocalDateTime now = LocalDateTime.now();
        String createdAtId = now.toString() + "#" + UUID.randomUUID();

        DynamoChatMessage item = new DynamoChatMessage();
        item.setRoomId(roomId);
        item.setCreatedAtId(createdAtId);
        item.setMemberId(memberId);
        item.setType(type.name());
        item.setContent(content);
        item.setExpiresAt(now.plusDays(TTL_DAYS).toInstant(ZoneOffset.UTC).getEpochSecond());

        table().putItem(item);

        return new ChatMessageResponse(createdAtId, roomId, memberId, nickname, type, content, now);
    }
}
