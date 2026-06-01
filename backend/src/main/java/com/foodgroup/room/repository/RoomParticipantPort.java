package com.foodgroup.room.repository;

import com.foodgroup.room.domain.RoomParticipant;

import java.util.List;
import java.util.Optional;

public interface RoomParticipantPort {
    RoomParticipant save(RoomParticipant participant);
    boolean existsByRoomIdAndMemberId(String roomId, String memberId);
    Optional<RoomParticipant> findByRoomIdAndMemberId(String roomId, String memberId);
    List<RoomParticipant> findByRoomId(String roomId);
    List<RoomParticipant> findByMemberId(String memberId);
    void delete(RoomParticipant participant);
}
