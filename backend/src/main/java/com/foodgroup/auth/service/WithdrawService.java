package com.foodgroup.auth.service;

import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.common.notification.NotificationPort;
import com.foodgroup.order.repository.OrderItemPort;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomParticipant;
import com.foodgroup.room.domain.RoomStatus;
import com.foodgroup.room.repository.RoomParticipantPort;
import com.foodgroup.room.repository.RoomPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final MemberPort memberPort;
    private final RoomPort roomPort;
    private final RoomParticipantPort roomParticipantPort;
    private final OrderItemPort orderItemPort;
    private final NotificationPort notificationPort;

    public void withdraw(String memberId) {
        List<RoomParticipant> participations = roomParticipantPort.findByMemberId(memberId);

        for (RoomParticipant participation : participations) {
            Room room = roomPort.findById(participation.getRoomId()).orElse(null);
            if (room == null) continue;

            if (room.getHostId().equals(memberId)) {
                handleHostWithdrawal(room);
            } else {
                handleParticipantWithdrawal(room, memberId, participation);
            }
        }

        memberPort.deleteById(memberId);
        log.info("회원 탈퇴 완료: {}", memberId);
    }

    private void handleHostWithdrawal(Room room) {
        if (room.getStatus() == RoomStatus.OPEN || room.getStatus() == RoomStatus.CLOSED) {
            notificationPort.sendToRoom(room.getId(), "방 취소 알림", "방장이 탈퇴하여 방이 취소되었습니다.");
            List<RoomParticipant> participants = roomParticipantPort.findByRoomId(room.getId());
            for (RoomParticipant p : participants) {
                orderItemPort.deleteByRoomIdAndMemberId(room.getId(), p.getMemberId());
                roomParticipantPort.delete(p);
            }
            room.updateStatus(RoomStatus.CANCELLED);
            roomPort.save(room);
        }
    }

    private void handleParticipantWithdrawal(Room room, String memberId, RoomParticipant participation) {
        RoomStatus status = room.getStatus();
        if (status == RoomStatus.OPEN || status == RoomStatus.CLOSED) {
            orderItemPort.deleteByRoomIdAndMemberId(room.getId(), memberId);
            room.decrementParticipantCount();
            roomPort.save(room);
        }
        roomParticipantPort.delete(participation);
    }
}
