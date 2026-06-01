package com.foodgroup.auth;

import com.foodgroup.auth.domain.Gender;
import com.foodgroup.auth.domain.Member;
import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.auth.service.AuthService;
import com.foodgroup.auth.service.DeviceTokenService;
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
    @Mock MemberPort memberPort;
    @Mock DeviceTokenService deviceTokenService;

    @Test
    void register_success() {
        given(memberPort.existsByNickname("홍길동")).willReturn(false);
        given(deviceTokenService.issueToken(any())).willReturn("device-token-123");
        given(memberPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        Member member = authService.register("홍길동");

        assertThat(member.getNickname()).isEqualTo("홍길동");
        assertThat(member.getDeviceToken()).isEqualTo("device-token-123");
    }

    @Test
    void register_duplicateNickname_throws() {
        given(memberPort.existsByNickname("홍길동")).willReturn(true);

        assertThatThrownBy(() -> authService.register("홍길동"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NICKNAME_DUPLICATE);
    }

    @Test
    void updateProfile_성별_주소_저장됨() {
        Member member = Member.builder().id("m-1").nickname("홍길동").deviceToken("d").build();
        given(memberPort.findById("m-1")).willReturn(Optional.of(member));
        given(memberPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        Member result = authService.updateProfile("m-1", Gender.MALE, "서울시 강남구");

        assertThat(result.getGender()).isEqualTo(Gender.MALE);
        assertThat(result.getAddress()).isEqualTo("서울시 강남구");
    }

    @Test
    void getMe_notFound_throws() {
        given(memberPort.findById("non-existent")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe("non-existent"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
