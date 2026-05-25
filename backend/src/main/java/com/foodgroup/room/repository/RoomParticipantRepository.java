package com.foodgroup.room.repository;

import com.foodgroup.room.domain.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {
    boolean existsByRoomIdAndMemberId(Long roomId, Long memberId);
    Optional<RoomParticipant> findByRoomIdAndMemberId(Long roomId, Long memberId);
    List<RoomParticipant> findByRoomId(Long roomId);

    @Query("SELECT COUNT(p) FROM RoomParticipant p WHERE p.roomId = :roomId")
    int countByRoomId(@Param("roomId") Long roomId);
}
