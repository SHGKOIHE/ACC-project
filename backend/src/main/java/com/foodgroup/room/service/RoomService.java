package com.foodgroup.room.service;

import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.common.notification.NotificationPort;
import com.foodgroup.room.domain.MeetingType;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomParticipant;
import com.foodgroup.room.domain.RoomStatus;
import com.foodgroup.order.repository.OrderItemRepository;
import com.foodgroup.room.repository.RoomParticipantRepository;
import com.foodgroup.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final OrderItemRepository orderItemRepository;
    private final RoomStateValidator stateValidator;
    private final NotificationPort notificationPort;

    @Transactional
    public Room createRoom(CreateRoomCommand cmd) {
        Room room = roomRepository.save(Room.builder()
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
                .build());

        roomParticipantRepository.save(RoomParticipant.builder()
                .roomId(room.getId())
                .memberId(cmd.hostId())
                .build());

        return room;
    }

    @Transactional(readOnly = true)
    public Room getRoom(Long roomId) {
        return findRoomOrThrow(roomId);
    }

    @Transactional(readOnly = true)
    public boolean isParticipant(Long roomId, Long memberId) {
        return roomParticipantRepository.existsByRoomIdAndMemberId(roomId, memberId);
    }

    @Transactional(readOnly = true)
    public List<Room> searchRooms(String category, MeetingType meetingType,
                                   Double lat, Double lng, Double radiusMeters) {
        List<Room> rooms = roomRepository.searchRooms(category, meetingType);
        if (meetingType == MeetingType.DINE_OUT && lat != null && lng != null) {
            double radius = (radiusMeters != null) ? radiusMeters : 1000.0;
            return rooms.stream()
                    .filter(r -> haversineMeters(lat, lng, r.getLatitude(), r.getLongitude()) <= radius)
                    .toList();
        }
        return rooms;
    }

    @Transactional
    public void joinRoom(Long roomId, Long memberId) {
        Room room = findRoomOrThrow(roomId);
        if (room.getStatus() != RoomStatus.OPEN) {
            throw new BusinessException(ErrorCode.ROOM_STATUS_INVALID);
        }
        if (roomParticipantRepository.existsByRoomIdAndMemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }
        if (room.getCurrentParticipantCount() >= room.getMaxParticipants()) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }
        roomParticipantRepository.save(RoomParticipant.builder()
                .roomId(roomId)
                .memberId(memberId)
                .build());
        room.incrementParticipantCount();
    }

    @Transactional
    public void leaveRoom(Long roomId, Long memberId) {
        Room room = findRoomOrThrow(roomId);
        if (room.getHostId().equals(memberId)) {
            throw new BusinessException(ErrorCode.HOST_CANNOT_LEAVE);
        }
        if (room.getStatus() != RoomStatus.OPEN) {
            throw new BusinessException(ErrorCode.ROOM_STATUS_INVALID);
        }
        RoomParticipant participant = roomParticipantRepository
                .findByRoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_ROOM_PARTICIPANT));
        roomParticipantRepository.delete(participant);
        room.decrementParticipantCount();
        orderItemRepository.deleteByRoomIdAndMemberId(roomId, memberId);
    }

    @Transactional
    public void closeRoom(Long roomId, Long hostId) {
        Room room = findRoomOrThrow(roomId);
        requireHost(room, hostId);
        if (!orderItemRepository.existsByRoomId(roomId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CONFIRMABLE);
        }
        validateAndTransition(room, RoomStatus.CLOSED);
    }

    @Transactional
    public void cancelRoom(Long roomId, Long hostId) {
        Room room = findRoomOrThrow(roomId);
        requireHost(room, hostId);
        if (room.getStatus() == RoomStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ROOM_COMPLETED);
        }
        validateAndTransition(room, RoomStatus.CANCELLED);
        notificationPort.sendToRoom(roomId, "방이 취소되었습니다",
                room.getTitle() + " 방이 방장에 의해 취소되었습니다.");
    }

    @Transactional
    public void completeRoom(Long roomId, Long hostId) {
        Room room = findRoomOrThrow(roomId);
        requireHost(room, hostId);
        validateAndTransition(room, RoomStatus.COMPLETED);
    }

    /** OrderService에서 주문 확정 후 호출 */
    @Transactional
    public void transitionToConfirmed(Long roomId) {
        Room room = findRoomOrThrow(roomId);
        validateAndTransition(room, RoomStatus.CONFIRMED);
    }

    // --- private helpers ---

    private void validateAndTransition(Room room, RoomStatus to) {
        if (!stateValidator.isValidTransition(room.getStatus(), to)) {
            throw new BusinessException(ErrorCode.ROOM_STATUS_INVALID);
        }
        room.updateStatus(to);
    }

    private void requireHost(Room room, Long memberId) {
        if (!room.getHostId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_HOST);
        }
    }

    private Room findRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
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
            Long hostId, String title, MeetingType meetingType,
            String restaurantName, String restaurantAddress, String restaurantCategory,
            Double latitude, Double longitude, Integer deliveryFee, Integer maxParticipants,
            LocalDateTime closedAt, String meetingAddress,
            String accountNumber, String accountHolder, String bankName
    ) {}
}
