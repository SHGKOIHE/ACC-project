package com.foodgroup.auth.repository;

public interface EmailSendPort {
    void sendVerificationCode(String email, String code);
}
