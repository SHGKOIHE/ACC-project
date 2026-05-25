package com.foodgroup.auth.service;

import com.foodgroup.auth.domain.Member;
import com.foodgroup.auth.repository.MemberRepository;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;

    @Transactional
    public Member register(String nickname, String deviceToken) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }

        return memberRepository.save(Member.builder()
                .nickname(nickname)
                .deviceToken(deviceToken)
                .build());
    }

    @Transactional(readOnly = true)
    public Member getMe(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional
    public void updateFcmToken(Long memberId, String fcmToken) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateFcmToken(fcmToken);
    }
}
