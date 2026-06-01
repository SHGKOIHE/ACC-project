package com.foodgroup.auth.controller;

import com.foodgroup.auth.domain.Gender;
import com.foodgroup.auth.domain.Member;
import com.foodgroup.auth.service.AuthService;
import com.foodgroup.auth.service.EmailVerificationService;
import com.foodgroup.auth.service.WithdrawService;
import com.foodgroup.common.dto.ApiResponse;
import com.foodgroup.common.security.MemberPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final WithdrawService withdrawService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> register(@Valid @RequestBody RegisterRequest request) {
        Member member = authService.register(request.nickname());
        return ApiResponse.ok(MemberResponse.from(member));
    }

    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMe(@AuthenticationPrincipal MemberPrincipal principal) {
        Member member = authService.getMe(principal.memberId());
        return ApiResponse.ok(MemberResponse.from(member));
    }

    @PutMapping("/me")
    public ApiResponse<MemberResponse> updateProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        Member member = authService.updateProfile(principal.memberId(), request.gender(), request.address());
        return ApiResponse.ok(MemberResponse.from(member));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal MemberPrincipal principal) {
        withdrawService.withdraw(principal.memberId());
    }

    @PostMapping("/email/send-code")
    public ApiResponse<Void> sendEmailCode(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody SendEmailCodeRequest request) {
        emailVerificationService.sendVerificationCode(principal.memberId(), request.email());
        return ApiResponse.ok(null);
    }

    @PostMapping("/email/verify")
    public ApiResponse<Void> verifyEmailCode(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verifyCode(principal.memberId(), request.email(), request.code());
        return ApiResponse.ok(null);
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 12)
            @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다")
            String nickname
    ) {}

    public record UpdateProfileRequest(
            Gender gender,
            @Size(max = 100) String address
    ) {}

    public record SendEmailCodeRequest(
            @NotBlank @Email String email
    ) {}

    public record VerifyEmailRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 6) String code
    ) {}

    public record MemberResponse(
            String id,
            String nickname,
            String gender,
            String address,
            String email,
            boolean emailVerified,
            String deviceToken
    ) {
        public static MemberResponse from(Member m) {
            return new MemberResponse(
                    m.getId(),
                    m.getNickname(),
                    m.getGender() != null ? m.getGender().name() : null,
                    m.getAddress(),
                    m.getEmail(),
                    m.isEmailVerified(),
                    m.getDeviceToken()
            );
        }
    }
}
