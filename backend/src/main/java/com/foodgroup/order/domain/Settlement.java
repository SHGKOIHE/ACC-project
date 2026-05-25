package com.foodgroup.order.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long roomId;

    @Column(nullable = false)
    private Integer totalMenuAmount;

    @Column(nullable = false)
    private Integer totalDeliveryFee;

    @Column(nullable = false)
    private Integer participantCount;

    @Column(nullable = false)
    private Integer deliveryFeePerPerson;

    @Column(nullable = false)
    private Integer hostSurplus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
