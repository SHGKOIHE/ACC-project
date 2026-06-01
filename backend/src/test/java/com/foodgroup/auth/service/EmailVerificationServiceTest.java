package com.foodgroup.auth.service;

import com.foodgroup.auth.domain.Member;
import com.foodgroup.auth.repository.EmailSendPort;
import com.foodgroup.auth.repository.EmailVerificationPort;
import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @InjectMocks EmailVerificationService emailVerificationService;
    @Mock MemberPort memberPort;
    @Mock EmailVerificationPort emailVerificationPort;
    @Mock EmailSendPort emailSendPort;

    @Test
    void sendCode_허용도메인_성공() {
        Member member = Member.builder().id("m-1").nickname("홍길동").deviceToken("d").build();
        given(memberPort.findById("m-1")).willReturn(Optional.of(member));

        emailVerificationService.sendVerificationCode("m-1", "user@khu.ac.kr");

        verify(emailVerificationPort).saveCode(eq("user@khu.ac.kr"), anyString());
        verify(emailSendPort).sendVerificationCode(eq("user@khu.ac.kr"), anyString());
    }

    @Test
    void sendCode_미허용도메인_예외() {
        assertThatThrownBy(() -> emailVerificationService.sendVerificationCode("m-1", "user@gmail.com"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_INVALID_DOMAIN);

        verify(memberPort, never()).findById(any());
    }

    @Test
    void sendCode_이미인증된경우_예외() {
        Member member = Member.builder().id("m-1").nickname("홍길동").deviceToken("d")
                .email("user@khu.ac.kr").emailVerified(true).build();
        given(memberPort.findById("m-1")).willReturn(Optional.of(member));

        assertThatThrownBy(() -> emailVerificationService.sendVerificationCode("m-1", "user@khu.ac.kr"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_VERIFIED);
    }

    @Test
    void verifyCode_올바른코드_이메일인증됨() {
        Member member = Member.builder().id("m-1").nickname("홍길동").deviceToken("d").build();
        given(memberPort.findById("m-1")).willReturn(Optional.of(member));
        given(emailVerificationPort.findCode("user@khu.ac.kr")).willReturn(Optional.of("123456"));
        given(memberPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        emailVerificationService.verifyCode("m-1", "user@khu.ac.kr", "123456");

        verify(memberPort).save(argThat(m -> m.isEmailVerified() && "user@khu.ac.kr".equals(m.getEmail())));
        verify(emailVerificationPort).deleteCode("user@khu.ac.kr");
    }

    @Test
    void verifyCode_틀린코드_예외() {
        given(emailVerificationPort.findCode("user@khu.ac.kr")).willReturn(Optional.of("123456"));

        assertThatThrownBy(() -> emailVerificationService.verifyCode("m-1", "user@khu.ac.kr", "000000"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_CODE_INVALID);
    }

    @Test
    void verifyCode_코드없음_만료예외() {
        given(emailVerificationPort.findCode("user@khu.ac.kr")).willReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verifyCode("m-1", "user@khu.ac.kr", "123456"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_CODE_EXPIRED);
    }
}
