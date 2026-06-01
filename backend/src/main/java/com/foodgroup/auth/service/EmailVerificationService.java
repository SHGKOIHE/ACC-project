package com.foodgroup.auth.service;

import com.foodgroup.auth.repository.EmailSendPort;
import com.foodgroup.auth.repository.EmailVerificationPort;
import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Set<String> ALLOWED_DOMAINS = Set.of(
            "khu.ac.kr", "snu.ac.kr", "yonsei.ac.kr", "korea.ac.kr",
            "kaist.ac.kr", "postech.ac.kr", "skku.edu", "hanyang.ac.kr",
            "sogang.ac.kr", "ewha.ac.kr", "cau.ac.kr", "inha.ac.kr",
            "ajou.ac.kr", "konkuk.ac.kr", "dongguk.edu", "hongik.ac.kr",
            "kookmin.ac.kr", "sungkyul.ac.kr", "gachon.ac.kr", "sejong.ac.kr"
    );

    private final MemberPort memberPort;
    private final EmailVerificationPort emailVerificationPort;
    private final EmailSendPort emailSendPort;
    private final SecureRandom random = new SecureRandom();

    public void sendVerificationCode(String memberId, String email) {
        validateDomain(email);
        var member = memberPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        emailVerificationPort.saveCode(email, code);
        emailSendPort.sendVerificationCode(email, code);
    }

    public void verifyCode(String memberId, String email, String code) {
        validateDomain(email);
        String stored = emailVerificationPort.findCode(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_CODE_EXPIRED));
        if (!stored.equals(code)) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_INVALID);
        }
        var member = memberPort.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.verifyEmail(email);
        memberPort.save(member);
        emailVerificationPort.deleteCode(email);
    }

    private void validateDomain(String email) {
        int at = email.lastIndexOf('@');
        if (at < 0) throw new BusinessException(ErrorCode.EMAIL_INVALID_DOMAIN);
        String domain = email.substring(at + 1).toLowerCase();
        if (!ALLOWED_DOMAINS.contains(domain)) {
            throw new BusinessException(ErrorCode.EMAIL_INVALID_DOMAIN);
        }
    }
}
