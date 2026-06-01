package com.foodgroup.room.service;

import com.foodgroup.room.domain.RoomStatus;
import org.springframework.stereotype.Component;

@Component
public class RoomStateValidator {

    public boolean isValidTransition(RoomStatus from, RoomStatus to) {
        return switch (from) {
            case OPEN      -> to == RoomStatus.CLOSED || to == RoomStatus.CANCELLED;
            case CLOSED    -> to == RoomStatus.CONFIRMED || to == RoomStatus.CANCELLED;
            case CONFIRMED -> to == RoomStatus.DELIVERING || to == RoomStatus.CANCELLED;
            case DELIVERING -> to == RoomStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
