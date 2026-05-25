package com.foodgroup.auth;

import com.foodgroup.auth.domain.Member;
import com.foodgroup.auth.repository.MemberRepository;
import com.foodgroup.auth.service.AuthService;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks AuthService authService;
    @Mock MemberRepository memberRepository;

    @Test
    void register_success() {
        given(memberRepository.existsByNickname("홍길동")).willReturn(false);
        given(memberRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        Member member = authService.register("홍길동", "device-uuid-123");

        assertThat(member.getNickname()).isEqualTo("홍길동");
        assertThat(member.getDeviceToken()).isEqualTo("device-uuid-123");
    }

    @Test
    void register_duplicateNickname_throws() {
        given(memberRepository.existsByNickname("홍길동")).willReturn(true);

        assertThatThrownBy(() -> authService.register("홍길동", "device-uuid-123"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NICKNAME_DUPLICATE);
    }

    @Test
    void getMe_notFound_throws() {
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
