package com.foodgroup.room.repository;

import com.foodgroup.common.util.AesEncryptor;
import com.foodgroup.room.domain.*;
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
public class DynamoRoomAdapter implements RoomPort {

    private static final String TABLE_NAME = "Rooms";

    private final DynamoDbEnhancedClient enhancedClient;
    private final AesEncryptor aesEncryptor;

    private DynamoDbTable<DynamoRoom> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoRoom.class));
    }

    @Override
    public Room save(Room room) {
        table().putItem(toItem(room));
        return room;
    }

    @Override
    public Optional<Room> findById(String id) {
        DynamoRoom item = table().getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(item).map(this::toDomain);
    }

    @Override
    public List<Room> scanByStatus(RoomStatus status) {
        Expression filter = Expression.builder()
                .expression("#s = :status")
                .expressionNames(Map.of("#s", "status"))
                .expressionValues(Map.of(":status", AttributeValue.builder().s(status.name()).build()))
                .build();
        return table().scan(ScanEnhancedRequest.builder().filterExpression(filter).build())
                .items().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Room> findOpenClosingBetween(LocalDateTime from, LocalDateTime to) {
        Expression filter = Expression.builder()
                .expression("#s = :open AND closedAt BETWEEN :from AND :to")
                .expressionNames(Map.of("#s", "status"))
                .expressionValues(Map.of(
                        ":open", AttributeValue.builder().s(RoomStatus.OPEN.name()).build(),
                        ":from", AttributeValue.builder().s(from.toString()).build(),
                        ":to", AttributeValue.builder().s(to.toString()).build()))
                .build();
        return table().scan(ScanEnhancedRequest.builder().filterExpression(filter).build())
                .items().stream().map(this::toDomain).toList();
    }

    @Override
    public int closeExpiredRooms(LocalDateTime now) {
        Expression filter = Expression.builder()
                .expression("#s = :open AND closedAt <= :now")
                .expressionNames(Map.of("#s", "status"))
                .expressionValues(Map.of(
                        ":open", AttributeValue.builder().s(RoomStatus.OPEN.name()).build(),
                        ":now", AttributeValue.builder().s(now.toString()).build()))
                .build();
        List<DynamoRoom> expired = table().scan(
                ScanEnhancedRequest.builder().filterExpression(filter).build())
                .items().stream().toList();
        String nowStr = now.toString();
        for (DynamoRoom r : expired) {
            r.setStatus(RoomStatus.CLOSED.name());
            r.setUpdatedAt(nowStr);
            table().putItem(r);
        }
        return expired.size();
    }

    private DynamoRoom toItem(Room r) {
        DynamoRoom item = new DynamoRoom();
        item.setId(r.getId());
        item.setHostId(r.getHostId());
        item.setTitle(r.getTitle());
        item.setMeetingType(r.getMeetingType() != null ? r.getMeetingType().name() : null);
        item.setRestaurantName(r.getRestaurantName());
        item.setRestaurantAddress(r.getRestaurantAddress());
        item.setRestaurantCategory(r.getRestaurantCategory());
        item.setLatitude(r.getLatitude());
        item.setLongitude(r.getLongitude());
        item.setDeliveryFee(r.getDeliveryFee());
        item.setMaxParticipants(r.getMaxParticipants());
        item.setCurrentParticipantCount(r.getCurrentParticipantCount());
        item.setStatus(r.getStatus() != null ? r.getStatus().name() : null);
        item.setClosedAt(r.getClosedAt() != null ? r.getClosedAt().toString() : null);
        item.setMeetingAddress(r.getMeetingAddress());
        item.setAccountNumber(r.getAccountNumber() != null ? aesEncryptor.encrypt(r.getAccountNumber()) : null);
        item.setAccountHolder(r.getAccountHolder());
        item.setBankName(r.getBankName());
        item.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        item.setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : LocalDateTime.now().toString());
        return item;
    }

    private Room toDomain(DynamoRoom item) {
        String accountNumber = null;
        if (item.getAccountNumber() != null) {
            try {
                accountNumber = aesEncryptor.decrypt(item.getAccountNumber());
            } catch (Exception e) {
                accountNumber = null;
            }
        }
        return Room.builder()
                .id(item.getId())
                .hostId(item.getHostId())
                .title(item.getTitle())
                .meetingType(item.getMeetingType() != null ? MeetingType.valueOf(item.getMeetingType()) : null)
                .restaurantName(item.getRestaurantName())
                .restaurantAddress(item.getRestaurantAddress())
                .restaurantCategory(item.getRestaurantCategory())
                .latitude(item.getLatitude())
                .longitude(item.getLongitude())
                .deliveryFee(item.getDeliveryFee())
                .maxParticipants(item.getMaxParticipants())
                .currentParticipantCount(item.getCurrentParticipantCount())
                .status(item.getStatus() != null ? RoomStatus.valueOf(item.getStatus()) : null)
                .closedAt(item.getClosedAt() != null ? DateTimeUtil.parse(item.getClosedAt()) : null)
                .meetingAddress(item.getMeetingAddress())
                .accountNumber(accountNumber)
                .accountHolder(item.getAccountHolder())
                .bankName(item.getBankName())
                .createdAt(item.getCreatedAt() != null ? DateTimeUtil.parse(item.getCreatedAt()) : null)
                .updatedAt(item.getUpdatedAt() != null ? DateTimeUtil.parse(item.getUpdatedAt()) : null)
                .build();
    }
}
