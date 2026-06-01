package com.foodgroup.ai;

import com.foodgroup.common.dto.ApiResponse;
import com.foodgroup.common.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class AiStandaloneRecommendController {

    private final AiRecommendClient aiRecommendClient;

    @PostMapping
    public ApiResponse<AiRecommendResponse> recommend(
            @RequestBody StandaloneRecommendRequest request,
            @AuthenticationPrincipal MemberPrincipal principal) {

        AiRecommendRequest lambdaRequest = new AiRecommendRequest(
                "standalone",
                List.of(),
                new AiRecommendRequest.FilterInfo(
                        request.category(),
                        request.maxDeliveryFee(),
                        request.userMessage(),
                        request.latitude(),
                        request.longitude()
                )
        );

        AiRecommendResponse response = aiRecommendClient.recommend(lambdaRequest);
        return ApiResponse.ok(response);
    }

    public record StandaloneRecommendRequest(
            Double latitude,
            Double longitude,
            String category,
            Integer maxDeliveryFee,
            String userMessage
    ) {}
}
