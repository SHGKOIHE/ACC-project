package com.foodgroup.chat.repository;

import com.foodgroup.chat.domain.ChatMessage;
import com.foodgroup.chat.domain.ChatMessageType;
import com.foodgroup.chat.domain.DynamoChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.time.LocalDateTime;
import com.foodgroup.common.util.DateTimeUtil;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DynamoChatMessageAdapter implements ChatMessagePort {

    private static final String TABLE_NAME = "ChatMessages";
    private static final long TTL_DAYS = 30;

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<DynamoChatMessage> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoChatMessage.class));
    }

    @Override
    public ChatMessage save(String roomId, String memberId, ChatMessageType type, String content) {
        LocalDateTime now = LocalDateTime.now();
        String sortKey = now.toString() + "#" + UUID.randomUUID();

        DynamoChatMessage item = new DynamoChatMessage();
        item.setRoomId(roomId);
        item.setCreatedAtId(sortKey);
        item.setMemberId(memberId);
        item.setType(type.name());
        item.setContent(content);
        item.setExpiresAt(now.plusDays(TTL_DAYS).toInstant(ZoneOffset.UTC).getEpochSecond());

        table().putItem(item);

        return ChatMessage.builder()
                .roomId(roomId)
                .memberId(memberId)
                .type(type)
                .content(content)
                .createdAt(now)
                .build();
    }

    @Override
    public List<ChatMessage> findTop50ByRoomId(String roomId) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(roomId).build()))
                .scanIndexForward(false)
                .limit(50)
                .build();

        return table().query(request).items().stream()
                .map(item -> {
                    String[] parts = item.getCreatedAtId().split("#");
                    LocalDateTime ts = DateTimeUtil.parse(parts[0]);
                    return ChatMessage.builder()
                            .roomId(item.getRoomId())
                            .memberId(item.getMemberId())
                            .type(ChatMessageType.valueOf(item.getType()))
                            .content(item.getContent())
                            .createdAt(ts)
                            .build();
                })
                .toList();
    }

    @Override
    public void deleteOldMessages(LocalDateTime cutoff) {
        // DynamoDB TTL handles automatic deletion — no-op here
    }
}
