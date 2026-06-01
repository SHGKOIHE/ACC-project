package com.foodgroup.order.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class DynamoOrderItem {

    private String id;
    private String roomId;
    private String memberId;
    private String menuName;
    private Integer quantity;
    private Integer price;
    private String createdAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDbSecondaryPartitionKey(indexNames = {"roomId-index"})
    @DynamoDbAttribute("roomId")
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    @DynamoDbAttribute("memberId")
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    @DynamoDbAttribute("menuName")
    public String getMenuName() { return menuName; }
    public void setMenuName(String menuName) { this.menuName = menuName; }

    @DynamoDbAttribute("quantity")
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    @DynamoDbAttribute("price")
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
