package com.foodgroup.order.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member_settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class MemberSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long settlementId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Integer menuAmount;

    @Column(nullable = false)
    private Integer deliveryFeeShare;

    @Column(nullable = false)
    private Integer totalAmount;

    @Column(nullable = false)
    private Boolean isHost;
}
