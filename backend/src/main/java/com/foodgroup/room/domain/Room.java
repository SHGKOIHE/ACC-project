package com.foodgroup.room.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Room {
    private String id;
    private String hostId;
    private String title;
    private MeetingType meetingType;
    private String restaurantName;
    private String restaurantAddress;
    private String restaurantCategory;
    private Double latitude;
    private Double longitude;
    private Integer deliveryFee;
    private Integer maxParticipants;
    private Integer currentParticipantCount;
    private RoomStatus status;
    private LocalDateTime closedAt;
    private String meetingAddress;
    private String accountNumber;
    private String accountHolder;
    private String bankName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void updateStatus(RoomStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void reopen() {
        this.status = RoomStatus.OPEN;
        this.closedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementParticipantCount() {
        this.currentParticipantCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void decrementParticipantCount() {
        this.currentParticipantCount--;
        this.updatedAt = LocalDateTime.now();
    }
}
