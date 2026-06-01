package com.foodgroup.auth.repository;

import java.util.Optional;

public interface EmailVerificationPort {
    void saveCode(String email, String code);
    Optional<String> findCode(String email);
    void deleteCode(String email);
}
