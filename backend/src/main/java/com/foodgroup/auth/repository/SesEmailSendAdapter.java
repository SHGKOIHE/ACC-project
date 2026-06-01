package com.foodgroup.auth.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

@Slf4j
@Component
@ConditionalOnProperty(name = "aws.ses.enabled", havingValue = "true")
public class SesEmailSendAdapter implements EmailSendPort {

    private final SesV2Client sesClient;
    private final String fromEmail;

    public SesEmailSendAdapter(SesV2Client sesClient,
                                @Value("${aws.ses.from-email}") String fromEmail) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendVerificationCode(String email, String code) {
        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(fromEmail)
                .destination(Destination.builder().toAddresses(email).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data("[FoodGroup] 이메일 인증 코드").charset("UTF-8").build())
                                .body(Body.builder()
                                        .text(Content.builder()
                                                .data("인증 코드: " + code + "\n\n5분 이내에 입력해 주세요.")
                                                .charset("UTF-8").build())
                                        .build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
        log.info("[SES] 인증 코드 발송 완료: {}", email);
    }
}
