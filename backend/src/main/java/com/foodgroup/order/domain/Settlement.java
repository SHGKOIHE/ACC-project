package com.foodgroup.order.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Settlement {
    private String id;
    private String roomId;
    private Integer totalMenuAmount;
    private Integer totalDeliveryFee;
    private Integer participantCount;
    private Integer deliveryFeePerPerson;
    private Integer hostSurplus;
    private LocalDateTime createdAt;
}
