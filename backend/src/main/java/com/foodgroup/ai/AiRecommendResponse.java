package com.foodgroup.ai;

import java.util.List;

public record AiRecommendResponse(
        List<RecommendationItem> recommendations,
        String explanation
) {
    public record RecommendationItem(
            int rank,
            String restaurantName,
            int score,
            String reason
    ) {}

    public static AiRecommendResponse empty() {
        return new AiRecommendResponse(List.of(), "");
    }
}
