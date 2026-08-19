package com.foodgroup.chat.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

// Minimal projection of backend's DynamoRoomParticipant — chat-server only needs an existence
// check (id = "{roomId}#{memberId}"), not the full participant record.
@DynamoDbBean
public class DynamoRoomParticipantKey {

    private String id;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
