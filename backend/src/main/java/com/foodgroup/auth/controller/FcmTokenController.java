package com.foodgroup.auth.controller;

import com.foodgroup.auth.service.AuthService;
import com.foodgroup.common.dto.ApiResponse;
import com.foodgroup.common.security.MemberPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class FcmTokenController {

    private final AuthService authService;

    @PutMapping("/fcm-token")
    public ApiResponse<Void> updateFcmToken(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody FcmTokenRequest request) {
        authService.updateFcmToken(principal.memberId(), request.fcmToken());
        return ApiResponse.ok();
    }

    record FcmTokenRequest(@NotBlank String fcmToken) {}
}
