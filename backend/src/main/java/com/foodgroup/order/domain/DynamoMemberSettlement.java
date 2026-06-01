package com.foodgroup.order.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class DynamoMemberSettlement {

    private String settlementId;
    private String memberId;
    private String id;
    private Integer menuAmount;
    private Integer deliveryFeeShare;
    private Integer totalAmount;
    private Boolean isHost;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("settlementId")
    public String getSettlementId() { return settlementId; }
    public void setSettlementId(String settlementId) { this.settlementId = settlementId; }

    @DynamoDbSortKey
    @DynamoDbAttribute("memberId")
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    @DynamoDbAttribute("id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDbAttribute("menuAmount")
    public Integer getMenuAmount() { return menuAmount; }
    public void setMenuAmount(Integer menuAmount) { this.menuAmount = menuAmount; }

    @DynamoDbAttribute("deliveryFeeShare")
    public Integer getDeliveryFeeShare() { return deliveryFeeShare; }
    public void setDeliveryFeeShare(Integer deliveryFeeShare) { this.deliveryFeeShare = deliveryFeeShare; }

    @DynamoDbAttribute("totalAmount")
    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }

    @DynamoDbAttribute("isHost")
    public Boolean getIsHost() { return isHost; }
    public void setIsHost(Boolean isHost) { this.isHost = isHost; }
}
