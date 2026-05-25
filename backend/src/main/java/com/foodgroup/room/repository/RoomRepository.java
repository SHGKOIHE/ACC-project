package com.foodgroup.room.repository;

import com.foodgroup.room.domain.MeetingType;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("""
            SELECT r FROM Room r
            WHERE r.status = 'OPEN'
            AND (:category IS NULL OR r.restaurantCategory = :category)
            AND (:meetingType IS NULL OR r.meetingType = :meetingType)
            ORDER BY r.createdAt DESC
            """)
    List<Room> searchRooms(@Param("category") String category,
                           @Param("meetingType") MeetingType meetingType);

    @Modifying
    @Query("""
            UPDATE Room r SET r.status = 'CLOSED'
            WHERE r.status = 'OPEN' AND r.closedAt <= :now
            """)
    int closeExpiredRooms(@Param("now") LocalDateTime now);

    List<Room> findByStatus(RoomStatus status);
}
