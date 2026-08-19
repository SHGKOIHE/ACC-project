package com.foodgroup.chat.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

// Mirrors backend's com.foodgroup.chat.domain.DynamoChatMessage — same "ChatMessages" table,
// same roomId/createdAtId key scheme, so backend's ChatHistoryController can read what this
// server writes.
@DynamoDbBean
public class DynamoChatMessage {

    private String roomId;
    private String createdAtId;
    private String memberId;
    private String type;
    private String content;
    private Long expiresAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("roomId")
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    @DynamoDbSortKey
    @DynamoDbAttribute("createdAtId")
    public String getCreatedAtId() { return createdAtId; }
    public void setCreatedAtId(String createdAtId) { this.createdAtId = createdAtId; }

    @DynamoDbAttribute("memberId")
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    @DynamoDbAttribute("type")
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @DynamoDbAttribute("content")
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @DynamoDbAttribute("expiresAt")
    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }
}
