package com.foodgroup.order.repository;

import com.foodgroup.order.domain.DynamoMemberSettlement;
import com.foodgroup.order.domain.MemberSettlement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DynamoMemberSettlementAdapter implements MemberSettlementPort {

    private static final String TABLE_NAME = "MemberSettlements";

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<DynamoMemberSettlement> table() {
        return enhancedClient.table(TABLE_NAME, TableSchema.fromBean(DynamoMemberSettlement.class));
    }

    @Override
    public List<MemberSettlement> saveAll(List<MemberSettlement> settlements) {
        for (MemberSettlement ms : settlements) {
            DynamoMemberSettlement item = new DynamoMemberSettlement();
            item.setSettlementId(ms.getSettlementId());
            item.setMemberId(ms.getMemberId());
            item.setId(ms.getId());
            item.setMenuAmount(ms.getMenuAmount());
            item.setDeliveryFeeShare(ms.getDeliveryFeeShare());
            item.setTotalAmount(ms.getTotalAmount());
            item.setIsHost(ms.getIsHost());
            table().putItem(item);
        }
        return settlements;
    }

    @Override
    public List<MemberSettlement> findBySettlementId(String settlementId) {
        QueryEnhancedRequest req = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(settlementId).build()))
                .build();
        return table().query(req).items().stream().map(this::toDomain).toList();
    }

    private MemberSettlement toDomain(DynamoMemberSettlement item) {
        return MemberSettlement.builder()
                .id(item.getId())
                .settlementId(item.getSettlementId())
                .memberId(item.getMemberId())
                .menuAmount(item.getMenuAmount())
                .deliveryFeeShare(item.getDeliveryFeeShare())
                .totalAmount(item.getTotalAmount())
                .isHost(item.getIsHost())
                .build();
    }
}
