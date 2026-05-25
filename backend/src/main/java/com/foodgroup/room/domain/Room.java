package com.foodgroup.room.domain;

import com.foodgroup.common.util.AesAttributeConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long hostId;

    @Column(nullable = false, length = 30)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingType meetingType;

    @Column(nullable = false)
    private String restaurantName;

    @Column(nullable = false)
    private String restaurantAddress;

    private String restaurantCategory;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Integer deliveryFee;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Integer currentParticipantCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    private LocalDateTime closedAt;
    private String meetingAddress;

    @Convert(converter = AesAttributeConverter.class)
    private String accountNumber;

    private String accountHolder;
    private String bankName;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = RoomStatus.OPEN;
        currentParticipantCount = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateStatus(RoomStatus newStatus) {
        this.status = newStatus;
    }

    public void incrementParticipantCount() {
        this.currentParticipantCount++;
    }

    public void decrementParticipantCount() {
        this.currentParticipantCount--;
    }
}
