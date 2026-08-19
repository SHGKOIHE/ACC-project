package com.foodgroup.chat.repository;

import com.foodgroup.chat.domain.DynamoRoomParticipantKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Component
@RequiredArgsConstructor
public class RoomParticipantChecker {

    private static final String TABLE_NAME = "RoomParticipants";

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<DynamoRoomParticipantKey> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoRoomParticipantKey.class));
    }

    public boolean isParticipant(String roomId, String memberId) {
        if (roomId == null || memberId == null) return false;
        return table().getItem(Key.builder().partitionValue(roomId + "#" + memberId).build()) != null;
    }
}
