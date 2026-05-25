package com.foodgroup.auth.repository;

import com.foodgroup.auth.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByDeviceToken(String deviceToken);
    boolean existsByNickname(String nickname);
}
