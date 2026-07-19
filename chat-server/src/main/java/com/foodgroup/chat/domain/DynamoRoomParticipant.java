package com.foodgroup.chat.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * RoomParticipants 테이블에서 참여 여부만 확인하기 위한 최소 매핑 빈.
 * PK는 backend와 동일하게 "roomId#memberId" 복합 문자열.
 */
@DynamoDbBean
public class DynamoRoomParticipant {

    private String id;
    private String roomId;
    private String memberId;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDbAttribute("roomId")
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    @DynamoDbAttribute("memberId")
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
}
