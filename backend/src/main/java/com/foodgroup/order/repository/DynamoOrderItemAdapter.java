package com.foodgroup.order.repository;

import com.foodgroup.order.domain.DynamoOrderItem;
import com.foodgroup.order.domain.OrderItem;
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
public class DynamoOrderItemAdapter implements OrderItemPort {

    private static final String TABLE_NAME = "OrderItems";

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<DynamoOrderItem> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoOrderItem.class));
    }

    private List<DynamoOrderItem> queryByRoomId(String roomId) {
        DynamoDbIndex<DynamoOrderItem> gsi = table().index("roomId-index");
        QueryEnhancedRequest req = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(roomId).build()))
                .build();
        return gsi.query(req).stream().flatMap(p -> p.items().stream()).toList();
    }

    @Override
    public OrderItem save(OrderItem item) {
        table().putItem(toItem(item));
        return item;
    }

    @Override
    public Optional<OrderItem> findById(String id) {
        DynamoOrderItem item = table().getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(item).map(this::toDomain);
    }

    @Override
    public List<OrderItem> findByRoomId(String roomId) {
        return queryByRoomId(roomId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<OrderItem> findByRoomIdAndMemberId(String roomId, String memberId) {
        return queryByRoomId(roomId).stream()
                .filter(i -> memberId.equals(i.getMemberId()))
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByRoomId(String roomId) {
        DynamoDbIndex<DynamoOrderItem> gsi = table().index("roomId-index");
        QueryEnhancedRequest req = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(roomId).build()))
                .limit(1)
                .build();
        return gsi.query(req).stream().flatMap(p -> p.items().stream()).findAny().isPresent();
    }

    @Override
    public boolean existsByRoomIdAndMemberId(String roomId, String memberId) {
        return queryByRoomId(roomId).stream().anyMatch(i -> memberId.equals(i.getMemberId()));
    }

    @Override
    public void deleteByRoomIdAndMemberId(String roomId, String memberId) {
        queryByRoomId(roomId).stream()
                .filter(i -> memberId.equals(i.getMemberId()))
                .forEach(i -> table().deleteItem(Key.builder().partitionValue(i.getId()).build()));
    }

    @Override
    public int sumAmountByRoomIdAndMemberId(String roomId, String memberId) {
        return queryByRoomId(roomId).stream()
                .filter(i -> memberId.equals(i.getMemberId()))
                .mapToInt(i -> i.getPrice() * i.getQuantity())
                .sum();
    }

    @Override
    public void delete(OrderItem item) {
        table().deleteItem(Key.builder().partitionValue(item.getId()).build());
    }

    private DynamoOrderItem toItem(OrderItem o) {
        DynamoOrderItem item = new DynamoOrderItem();
        item.setId(o.getId());
        item.setRoomId(o.getRoomId());
        item.setMemberId(o.getMemberId());
        item.setMenuName(o.getMenuName());
        item.setQuantity(o.getQuantity());
        item.setPrice(o.getPrice());
        item.setCreatedAt(o.getCreatedAt() != null ? o.getCreatedAt().toString() : LocalDateTime.now().toString());
        return item;
    }

    private OrderItem toDomain(DynamoOrderItem i) {
        return OrderItem.builder()
                .id(i.getId())
                .roomId(i.getRoomId())
                .memberId(i.getMemberId())
                .menuName(i.getMenuName())
                .quantity(i.getQuantity())
                .price(i.getPrice())
                .createdAt(i.getCreatedAt() != null ? DateTimeUtil.parse(i.getCreatedAt()) : null)
                .build();
    }
}
