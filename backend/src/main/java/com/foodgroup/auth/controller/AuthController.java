package com.foodgroup.auth.controller;

import com.foodgroup.auth.domain.Member;
import com.foodgroup.auth.service.AuthService;
import com.foodgroup.common.dto.ApiResponse;
import com.foodgroup.common.security.MemberPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "회원 인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원 등록")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> register(@Valid @RequestBody RegisterRequest request) {
        Member member = authService.register(request.nickname(), request.deviceToken());
        return ApiResponse.ok(MemberResponse.from(member));
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMe(@AuthenticationPrincipal MemberPrincipal principal) {
        Member member = authService.getMe(principal.memberId());
        return ApiResponse.ok(MemberResponse.from(member));
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 12)
            @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다")
            String nickname,

            @NotBlank String deviceToken
    ) {}

    public record MemberResponse(Long id, String nickname) {
        public static MemberResponse from(Member m) {
            return new MemberResponse(m.getId(), m.getNickname());
        }
    }
}
