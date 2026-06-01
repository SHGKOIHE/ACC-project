package com.foodgroup.auth.service;

import com.foodgroup.auth.domain.Gender;
import com.foodgroup.auth.domain.Member;
import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberPort memberPort;
    private final DeviceTokenService deviceTokenService;

    public Member register(String nickname) {
        if (memberPort.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }
        String memberId = UUID.randomUUID().toString();
        String deviceToken = deviceTokenService.issueToken(memberId);
        return memberPort.save(Member.builder()
                .id(memberId)
                .nickname(nickname)
                .deviceToken(deviceToken)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public Member getMe(String memberId) {
        return memberPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public void updateFcmToken(String memberId, String fcmToken) {
        Member member = memberPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateFcmToken(fcmToken);
        memberPort.save(member);
    }

    public Member updateProfile(String memberId, Gender gender, String address) {
        Member member = memberPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateProfile(gender, address);
        return memberPort.save(member);
    }
}
