package com.foodgroup.auth.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class DynamoMember {

    private String id;
    private String nickname;
    private String deviceToken;
    private String fcmToken;
    private String gender;
    private String address;
    private String email;
    private Boolean emailVerified;
    private String createdAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDbAttribute("nickname")
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    @DynamoDbSecondaryPartitionKey(indexNames = {"deviceToken-index"})
    @DynamoDbAttribute("deviceToken")
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }

    @DynamoDbAttribute("fcmToken")
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    @DynamoDbAttribute("gender")
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    @DynamoDbAttribute("address")
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @DynamoDbAttribute("email")
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @DynamoDbAttribute("emailVerified")
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
