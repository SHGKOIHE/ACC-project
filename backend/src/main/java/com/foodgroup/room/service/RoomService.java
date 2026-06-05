package com.foodgroup.room.service;

import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.common.notification.NotificationPort;
import com.foodgroup.order.repository.OrderItemPort;
import com.foodgroup.room.domain.MeetingType;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomParticipant;
import com.foodgroup.room.domain.RoomStatus;
import com.foodgroup.room.repository.RoomParticipantPort;
import com.foodgroup.room.repository.RoomPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomPort roomPort;
    private final RoomParticipantPort roomParticipantPort;
    private final OrderItemPort orderItemPort;
    private final RoomStateValidator stateValidator;
    private final NotificationPort notificationPort;
    private final MemberPort memberPort;

    public Room createRoom(CreateRoomCommand cmd) {
        if (hasActiveRoom(cmd.hostId())) {
            throw new BusinessException(ErrorCode.ALREADY_IN_ACTIVE_ROOM);
        }
        LocalDateTime now = LocalDateTime.now();
        Room room = roomPort.save(Room.builder()
                .id(UUID.randomUUID().toString())
                .hostId(cmd.hostId())
                .title(cmd.title())
                .meetingType(cmd.meetingType())
                .restaurantName(cmd.restaurantName())
                .restaurantAddress(cmd.restaurantAddress())
                .restaurantCategory(cmd.restaurantCategory())
                .latitude(cmd.latitude())
                .longitude(cmd.longitude())
                .deliveryFee(cmd.deliveryFee())
                .maxParticipants(cmd.maxParticipants())
                .closedAt(cmd.closedAt())
                .meetingAddress(cmd.meetingAddress())
                .accountNumber(cmd.accountNumber())
                .accountHolder(cmd.accountHolder())
                .bankName(cmd.bankName())
                .status(RoomStatus.OPEN)
                .currentParticipantCount(1)
                .createdAt(now)
                .updatedAt(now)
                .build());

        roomParticipantPort.save(RoomParticipant.builder()
                .id(UUID.randomUUID().toString())
                .roomId(room.getId())
                .memberId(cmd.hostId())
                .joinedAt(now)
                .build());

        return room;
    }

    public Room getRoom(String roomId) {
        return findRoomOrThrow(roomId);
    }

    public boolean isParticipant(String roomId, String memberId) {
        return roomParticipantPort.existsByRoomIdAndMemberId(roomId, memberId);
    }

    public List<Room> searchRooms(String category, MeetingType meetingType,
                                   Double lat, Double lng, Double radiusMeters, String memberId) {
        LinkedHashMap<String, Room> visibleRooms = new LinkedHashMap<>();
        roomPort.scanByStatus(RoomStatus.OPEN).forEach(room -> visibleRooms.put(room.getId(), room));
        if (memberId != null && !memberId.isBlank()) {
            roomParticipantPort.findByMemberId(memberId).stream()
                    .map(participant -> roomPort.findById(participant.getRoomId()))
                    .flatMap(Optional::stream)
                    .filter(this::isActiveRoom)
                    .forEach(room -> visibleRooms.put(room.getId(), room));
        }
        return visibleRooms.values().stream()
                .filter(r -> category == null || category.equals(r.getRestaurantCategory()))
                .filter(r -> meetingType == null || meetingType == r.getMeetingType())
                .filter(r -> {
                    if (meetingType == MeetingType.DINE_OUT && lat != null && lng != null) {
                        double radius = (radiusMeters != null) ? radiusMeters : 1000.0;
                        return haversineMeters(lat, lng, r.getLatitude(), r.getLongitude()) <= radius;
                    }
                    return true;
                })
                .toList();
    }

    public List<Room> searchRooms(String category, MeetingType meetingType,
                                   Double lat, Double lng, Double radiusMeters) {
        return searchRooms(category, meetingType, lat, lng, radiusMeters, null);
    }

    public void joinRoom(String roomId, String memberId) {
        Room room = findRoomOrThrow(roomId);
        if (room.getStatus() != RoomStatus.OPEN) {
            throw new BusinessException(ErrorCode.ROOM_STATUS_INVALID);
        }
        if (roomParticipantPort.existsByRoomIdAndMemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }
        if (hasActiveRoom(memberId)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_ACTIVE_ROOM);
        }
        if (room.getCurrentParticipantCount() >= room.getMaxParticipants()) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }
        roomParticipantPort.save(RoomParticipant.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .memberId(memberId)
                .joinedAt(LocalDateTime.now())
                .build());
        room.incrementParticipantCount();
        roomPort.save(room);
    }

    public void leaveRoom(String roomId, String memberId) {
        Room room = findRoomOrThrow(roomId);
        if (room.getStatus() != RoomStatus.OPEN) {
            throw new BusinessException(ErrorCode.ROOM_STATUS_INVALID);
        }
        if (room.getHostId().equals(memberId)) {
            boolean hasOtherParticipants = roomParticipantPort.findByRoomId(roomId).stream()
                    .anyMatch(participant -> !participant.getMemberId().equals(memberId));
            if (hasOtherParticipants) {
                throw new BusinessException(ErrorCode.HOST_CANNOT_LEAVE);
            }
            roomParticipantPort.findByRoomIdAndMemberId(roomId, memberId)
                    .ifPresent(roomParticipantPort::delete);
            orderItemPort.deleteByRoomIdAndMemberId(roomId, memberId);
            roomPort.delete(roomId);
            return;
        }
        RoomParticipant participant = roomParticipantPort
                .findByRoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_ROOM_PARTICIPANT));
        roomParticipantPort.delete(participant);
        room.decrementParticipantCount();
        roomPort.save(room);
        orderItemPort.deleteByRoomIdAndMemberId(roomId, memberId);
    }

    public void closeRoom(String roomId, String hostId) {
        Room room = findRoomOrThrow(roomId);
        requireHost(room, hostId);
        if (!orderItemPort.existsByRoomId(roomId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CONFIRMABLE);
        }
        requireEveryParticipantHasOrder(roomId);
        validateAndTransition(room, RoomStatus.CLOSED);
        roomPort.save(room);
    }

    public void reopenRoom(String roomId, String hostId) {
        Room room = findRoomOrThrow(roomId);
        requireHost(room, hostId);
        if (!stateValidator.isValidTransition(room.getStatus(), RoomStatus.OPEN)) {
            throw new BusinessException(ErrorCode.ROOM_STATUS_INVALID);
        }
        room.reopen();
        roomPort.save(room);
    }

    public void cancelRoom(String roomId, String hostId) {
        Room room = findRoomOrThrow(roomId);
        requireHost(room, hostId);
        if (room.getStatus() == RoomStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ROOM_COMPLETED);
        }
        validateAndTransition(room, RoomStatus.CANCELLED);
        roomPort.save(room);
        notificationPort.sendToRoom(roomId, "방이 취소되었습니다",
                room.getTitle() + " 방이 방장에 의해 취소되었습니다.");
    }

    public void deliverRoom(String roomId, String hostId) {
        Room room = findRoomOrThrow(roomId);
        requireHost(room, hostId);
        validateAndTransition(room, RoomStatus.DELIVERING);
        roomPort.save(room);
        notificationPort.sendToRoom(roomId, "배달 시작",
                room.getTitle() + " 방의 배달이 시작되었습니다.");
    }

    public void completeRoom(String roomId, String hostId) {
        Room room = findRoomOrThrow(roomId);
        requireHost(room, hostId);
        validateAndTransition(room, RoomStatus.COMPLETED);
        roomPort.save(room);
        notificationPort.sendToRoom(roomId, "배달 완료",
                room.getTitle() + " 방의 배달이 완료되었습니다. 정산 내역을 확인하세요.");
    }

    public void transitionToConfirmed(String roomId) {
        Room room = findRoomOrThrow(roomId);
        validateAndTransition(room, RoomStatus.CONFIRMED);
        roomPort.save(room);
    }

    public List<ParticipantInfo> getParticipants(String roomId) {
        Room room = findRoomOrThrow(roomId);
        return roomParticipantPort.findByRoomId(roomId).stream()
                .map(p -> {
                    String nickname = memberPort.findById(p.getMemberId())
                            .map(m -> m.getNickname())
                            .orElse(p.getMemberId());
                    return new ParticipantInfo(p.getMemberId(), nickname, p.getMemberId().equals(room.getHostId()));
                })
                .toList();
    }

    public record ParticipantInfo(String memberId, String nickname, boolean isHost) {}

    // --- private helpers ---

    private void validateAndTransition(Room room, RoomStatus to) {
        if (!stateValidator.isValidTransition(room.getStatus(), to)) {
            throw new BusinessException(ErrorCode.ROOM_STATUS_INVALID);
        }
        room.updateStatus(to);
    }

    private void requireHost(Room room, String memberId) {
        if (!room.getHostId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_HOST);
        }
    }

    private Room findRoomOrThrow(String roomId) {
        return roomPort.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    private void requireEveryParticipantHasOrder(String roomId) {
        for (RoomParticipant participant : roomParticipantPort.findByRoomId(roomId)) {
            if (!orderItemPort.existsByRoomIdAndMemberId(roomId, participant.getMemberId())) {
                throw new BusinessException(ErrorCode.ORDER_NOT_CONFIRMABLE);
            }
        }
    }

    private boolean hasActiveRoom(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            return false;
        }
        return roomParticipantPort.findByMemberId(memberId).stream()
                .map(participant -> roomPort.findById(participant.getRoomId()))
                .flatMap(Optional::stream)
                .anyMatch(this::isActiveRoom);
    }

    private boolean isActiveRoom(Room room) {
        return room.getStatus() == RoomStatus.OPEN
                || room.getStatus() == RoomStatus.CLOSED
                || room.getStatus() == RoomStatus.CONFIRMED
                || room.getStatus() == RoomStatus.DELIVERING;
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public record CreateRoomCommand(
            String hostId, String title, MeetingType meetingType,
            String restaurantName, String restaurantAddress, String restaurantCategory,
            Double latitude, Double longitude, Integer deliveryFee, Integer maxParticipants,
            LocalDateTime closedAt, String meetingAddress,
            String accountNumber, String accountHolder, String bankName
    ) {}
}
