package com.foodgroup.auth.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@ConditionalOnProperty(name = "aws.ses.enabled", havingValue = "false", matchIfMissing = true)
@Component
public class LogEmailSendAdapter implements EmailSendPort {

    @Override
    public void sendVerificationCode(String email, String code) {
        log.info("[이메일 인증] {} 로 인증 코드 발송: {}", email, code);
    }
}
