package com.foodgroup.order.repository;

import com.foodgroup.order.domain.DynamoSettlement;
import com.foodgroup.order.domain.Settlement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;

import java.time.LocalDateTime;
import com.foodgroup.common.util.DateTimeUtil;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DynamoSettlementAdapter implements SettlementPort {

    private static final String TABLE_NAME = "Settlements";

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<DynamoSettlement> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoSettlement.class));
    }

    @Override
    public Settlement save(Settlement settlement) {
        DynamoSettlement item = new DynamoSettlement();
        item.setRoomId(settlement.getRoomId());
        item.setId(settlement.getId());
        item.setTotalMenuAmount(settlement.getTotalMenuAmount());
        item.setTotalDeliveryFee(settlement.getTotalDeliveryFee());
        item.setParticipantCount(settlement.getParticipantCount());
        item.setDeliveryFeePerPerson(settlement.getDeliveryFeePerPerson());
        item.setHostSurplus(settlement.getHostSurplus());
        item.setCreatedAt(settlement.getCreatedAt() != null ? settlement.getCreatedAt().toString() : LocalDateTime.now().toString());
        table().putItem(item);
        return settlement;
    }

    @Override
    public Optional<Settlement> findByRoomId(String roomId) {
        DynamoSettlement item = table().getItem(Key.builder().partitionValue(roomId).build());
        return Optional.ofNullable(item).map(this::toDomain);
    }

    @Override
    public Optional<Settlement> findById(String id) {
        // Settlement PK is roomId; findById by UUID is not supported without GSI
        // This method is not used in current service layer
        return Optional.empty();
    }

    private Settlement toDomain(DynamoSettlement item) {
        return Settlement.builder()
                .id(item.getId())
                .roomId(item.getRoomId())
                .totalMenuAmount(item.getTotalMenuAmount())
                .totalDeliveryFee(item.getTotalDeliveryFee())
                .participantCount(item.getParticipantCount())
                .deliveryFeePerPerson(item.getDeliveryFeePerPerson())
                .hostSurplus(item.getHostSurplus())
                .createdAt(item.getCreatedAt() != null ? DateTimeUtil.parse(item.getCreatedAt()) : null)
                .build();
    }
}
