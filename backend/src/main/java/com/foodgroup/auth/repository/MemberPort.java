package com.foodgroup.auth.repository;

import com.foodgroup.auth.domain.Member;

import java.util.Optional;

public interface MemberPort {
    Member save(Member member);
    Optional<Member> findById(String id);
    Optional<Member> findByDeviceToken(String deviceToken);
    boolean existsByNickname(String nickname);
    void deleteById(String id);
}
