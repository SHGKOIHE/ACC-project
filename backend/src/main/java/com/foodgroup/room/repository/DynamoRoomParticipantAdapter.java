package com.foodgroup.room.repository;

import com.foodgroup.room.domain.DynamoRoomParticipant;
import com.foodgroup.room.domain.RoomParticipant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import com.foodgroup.common.util.DateTimeUtil;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DynamoRoomParticipantAdapter implements RoomParticipantPort {

    private static final String TABLE_NAME = "RoomParticipants";

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<DynamoRoomParticipant> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoRoomParticipant.class));
    }

    private static String compositeId(String roomId, String memberId) {
        return roomId + "#" + memberId;
    }

    @Override
    public RoomParticipant save(RoomParticipant participant) {
        DynamoRoomParticipant item = new DynamoRoomParticipant();
        item.setId(compositeId(participant.getRoomId(), participant.getMemberId()));
        item.setRoomId(participant.getRoomId());
        item.setMemberId(participant.getMemberId());
        item.setJoinedAt(participant.getJoinedAt() != null ? participant.getJoinedAt().toString() : LocalDateTime.now().toString());
        table().putItem(item);
        return participant;
    }

    @Override
    public boolean existsByRoomIdAndMemberId(String roomId, String memberId) {
        return table().getItem(Key.builder()
                .partitionValue(compositeId(roomId, memberId)).build()) != null;
    }

    @Override
    public Optional<RoomParticipant> findByRoomIdAndMemberId(String roomId, String memberId) {
        DynamoRoomParticipant item = table().getItem(Key.builder()
                .partitionValue(compositeId(roomId, memberId)).build());
        return Optional.ofNullable(item).map(this::toDomain);
    }

    @Override
    public List<RoomParticipant> findByRoomId(String roomId) {
        DynamoDbIndex<DynamoRoomParticipant> gsi = table().index("roomId-index");
        return gsi.query(QueryEnhancedRequest.builder()
                        .queryConditional(QueryConditional.keyEqualTo(
                                Key.builder().partitionValue(roomId).build()))
                        .build())
                .stream()
                .flatMap(p -> p.items().stream())
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<RoomParticipant> findByMemberId(String memberId) {
        Expression filter = Expression.builder()
                .expression("memberId = :memberId")
                .expressionValues(Map.of(":memberId", AttributeValue.builder().s(memberId).build()))
                .build();
        return table().scan(ScanEnhancedRequest.builder().filterExpression(filter).build())
                .items().stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(RoomParticipant participant) {
        table().deleteItem(Key.builder()
                .partitionValue(compositeId(participant.getRoomId(), participant.getMemberId()))
                .build());
    }

    private RoomParticipant toDomain(DynamoRoomParticipant item) {
        return RoomParticipant.builder()
                .id(item.getId())
                .roomId(item.getRoomId())
                .memberId(item.getMemberId())
                .joinedAt(item.getJoinedAt() != null ? DateTimeUtil.parse(item.getJoinedAt()) : null)
                .build();
    }
}
