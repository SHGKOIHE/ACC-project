package com.foodgroup.ai;

import java.util.List;

public record AiRecommendRequest(
        Long roomId,
        List<ParticipantInfo> participants,
        FilterInfo filters
) {
    public record ParticipantInfo(
            String nickname,
            List<OrderItemInfo> orderItems
    ) {}

    public record OrderItemInfo(
            String name,
            int price
    ) {}

    public record FilterInfo(
            String category,
            Integer maxDeliveryFee
    ) {}
}
