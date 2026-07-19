package com.foodgroup.chat.repository;

import com.foodgroup.chat.domain.DynamoRoomParticipant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RoomParticipantLookup {

    private static final String TABLE_NAME = "RoomParticipants";

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<DynamoRoomParticipant> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoRoomParticipant.class));
    }

    public boolean isParticipant(String roomId, String memberId) {
        if (roomId == null || roomId.isBlank() || memberId == null || memberId.isBlank()) {
            return false;
        }
        try {
            DynamoRoomParticipant item = table().getItem(
                    Key.builder().partitionValue(roomId + "#" + memberId).build());
            return item != null;
        } catch (Exception e) {
            return false;
        }
    }
}
