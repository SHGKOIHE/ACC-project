package com.foodgroup.order.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class DynamoSettlement {

    private String roomId;   // PK — unique per settlement
    private String id;       // UUID for external reference
    private Integer totalMenuAmount;
    private Integer totalDeliveryFee;
    private Integer participantCount;
    private Integer deliveryFeePerPerson;
    private Integer hostSurplus;
    private String createdAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("roomId")
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    @DynamoDbAttribute("id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDbAttribute("totalMenuAmount")
    public Integer getTotalMenuAmount() { return totalMenuAmount; }
    public void setTotalMenuAmount(Integer totalMenuAmount) { this.totalMenuAmount = totalMenuAmount; }

    @DynamoDbAttribute("totalDeliveryFee")
    public Integer getTotalDeliveryFee() { return totalDeliveryFee; }
    public void setTotalDeliveryFee(Integer totalDeliveryFee) { this.totalDeliveryFee = totalDeliveryFee; }

    @DynamoDbAttribute("participantCount")
    public Integer getParticipantCount() { return participantCount; }
    public void setParticipantCount(Integer participantCount) { this.participantCount = participantCount; }

    @DynamoDbAttribute("deliveryFeePerPerson")
    public Integer getDeliveryFeePerPerson() { return deliveryFeePerPerson; }
    public void setDeliveryFeePerPerson(Integer deliveryFeePerPerson) { this.deliveryFeePerPerson = deliveryFeePerPerson; }

    @DynamoDbAttribute("hostSurplus")
    public Integer getHostSurplus() { return hostSurplus; }
    public void setHostSurplus(Integer hostSurplus) { this.hostSurplus = hostSurplus; }

    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
