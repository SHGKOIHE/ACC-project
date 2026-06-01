package com.foodgroup.room.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class DynamoRoom {

    private String id;
    private String hostId;
    private String title;
    private String meetingType;
    private String restaurantName;
    private String restaurantAddress;
    private String restaurantCategory;
    private Double latitude;
    private Double longitude;
    private Integer deliveryFee;
    private Integer maxParticipants;
    private Integer currentParticipantCount;
    private String status;
    private String closedAt;
    private String meetingAddress;
    private String accountNumber;   // AES-encrypted
    private String accountHolder;
    private String bankName;
    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDbAttribute("hostId")
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }

    @DynamoDbAttribute("title")
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @DynamoDbAttribute("meetingType")
    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    @DynamoDbAttribute("restaurantName")
    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    @DynamoDbAttribute("restaurantAddress")
    public String getRestaurantAddress() { return restaurantAddress; }
    public void setRestaurantAddress(String restaurantAddress) { this.restaurantAddress = restaurantAddress; }

    @DynamoDbAttribute("restaurantCategory")
    public String getRestaurantCategory() { return restaurantCategory; }
    public void setRestaurantCategory(String restaurantCategory) { this.restaurantCategory = restaurantCategory; }

    @DynamoDbAttribute("latitude")
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    @DynamoDbAttribute("longitude")
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    @DynamoDbAttribute("deliveryFee")
    public Integer getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(Integer deliveryFee) { this.deliveryFee = deliveryFee; }

    @DynamoDbAttribute("maxParticipants")
    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    @DynamoDbAttribute("currentParticipantCount")
    public Integer getCurrentParticipantCount() { return currentParticipantCount; }
    public void setCurrentParticipantCount(Integer currentParticipantCount) { this.currentParticipantCount = currentParticipantCount; }

    @DynamoDbAttribute("status")
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @DynamoDbAttribute("closedAt")
    public String getClosedAt() { return closedAt; }
    public void setClosedAt(String closedAt) { this.closedAt = closedAt; }

    @DynamoDbAttribute("meetingAddress")
    public String getMeetingAddress() { return meetingAddress; }
    public void setMeetingAddress(String meetingAddress) { this.meetingAddress = meetingAddress; }

    @DynamoDbAttribute("accountNumber")
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    @DynamoDbAttribute("accountHolder")
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }

    @DynamoDbAttribute("bankName")
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @DynamoDbAttribute("updatedAt")
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
