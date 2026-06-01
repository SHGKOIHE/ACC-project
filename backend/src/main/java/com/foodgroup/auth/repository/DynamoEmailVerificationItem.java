package com.foodgroup.auth.repository;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class DynamoEmailVerificationItem {
    private String email;
    private String code;
    private Long ttl;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("email")
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @DynamoDbAttribute("code")
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    @DynamoDbAttribute("ttl")
    public Long getTtl() { return ttl; }
    public void setTtl(Long ttl) { this.ttl = ttl; }
}
