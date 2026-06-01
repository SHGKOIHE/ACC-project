package com.foodgroup.room.repository;

import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomPort {
    Room save(Room room);
    Optional<Room> findById(String id);
    List<Room> scanByStatus(RoomStatus status);
    List<Room> findOpenClosingBetween(LocalDateTime from, LocalDateTime to);
    int closeExpiredRooms(LocalDateTime now);
}
