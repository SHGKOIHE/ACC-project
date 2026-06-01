package com.foodgroup.room.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class RoomParticipant {
    private String id;
    private String roomId;
    private String memberId;
    private LocalDateTime joinedAt;
}
