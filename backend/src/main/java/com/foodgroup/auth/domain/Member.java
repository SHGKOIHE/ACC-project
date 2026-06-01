package com.foodgroup.auth.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Member {
    private String id;
    private String nickname;
    private String deviceToken;
    private String fcmToken;
    private Gender gender;
    private String address;
    private String email;
    private boolean emailVerified;
    private LocalDateTime createdAt;

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void updateProfile(Gender gender, String address) {
        this.gender = gender;
        this.address = address;
    }

    public void verifyEmail(String email) {
        this.email = email;
        this.emailVerified = true;
    }
}
