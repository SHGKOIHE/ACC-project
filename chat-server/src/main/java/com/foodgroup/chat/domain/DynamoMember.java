package com.foodgroup.chat.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Members 테이블에서 닉네임만 읽기 위한 최소 매핑 빈.
 * 테이블에 다른 속성이 있어도 Enhanced Client가 조회 시 무시한다.
 */
@DynamoDbBean
public class DynamoMember {

    private String id;
    private String nickname;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDbAttribute("nickname")
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
