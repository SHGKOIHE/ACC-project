package com.foodgroup.order.domain;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class MemberSettlement {
    private String id;
    private String settlementId;
    private String memberId;
    private Integer menuAmount;
    private Integer deliveryFeeShare;
    private Integer totalAmount;
    private Boolean isHost;
}
