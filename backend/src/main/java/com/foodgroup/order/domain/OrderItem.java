package com.foodgroup.order.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class OrderItem {
    private String id;
    private String roomId;
    private String memberId;
    private String menuName;
    private Integer quantity;
    private Integer price;
    private LocalDateTime createdAt;
}
