package com.foodgroup.order.service;

import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.common.notification.NotificationPort;
import com.foodgroup.order.domain.MemberSettlement;
import com.foodgroup.order.domain.OrderItem;
import com.foodgroup.order.domain.Settlement;
import com.foodgroup.order.repository.MemberSettlementPort;
import com.foodgroup.order.repository.OrderItemPort;
import com.foodgroup.order.repository.SettlementPort;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomParticipant;
import com.foodgroup.room.domain.RoomStatus;
import com.foodgroup.room.repository.RoomParticipantPort;
import com.foodgroup.room.repository.RoomPort;
import com.foodgroup.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderItemPort orderItemPort;
    private final SettlementPort settlementPort;
    private final MemberSettlementPort memberSettlementPort;
    private final RoomPort roomPort;
    private final RoomParticipantPort roomParticipantPort;
    private final DeliveryFeeCalculator deliveryFeeCalculator;
    private final RoomService roomService;
    private final NotificationPort notificationPort;

    public OrderItem addOrderItem(String roomId, String memberId,
                                   String menuName, int quantity, int price) {
        Room room = findRoomOrThrow(roomId);
        if (room.getStatus() == RoomStatus.CONFIRMED
                || room.getStatus() == RoomStatus.COMPLETED
                || room.getStatus() == RoomStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_LOCKED);
        }
        if (!roomParticipantPort.existsByRoomIdAndMemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_PARTICIPANT);
        }
        return orderItemPort.save(OrderItem.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId).memberId(memberId)
                .menuName(menuName).quantity(quantity).price(price)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void deleteOrderItem(String roomId, String orderItemId, String requesterId) {
        OrderItem item = orderItemPort.findById(orderItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));
        if (!item.getRoomId().equals(roomId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Room room = findRoomOrThrow(item.getRoomId());
        if (room.getStatus() == RoomStatus.CONFIRMED || room.getStatus() == RoomStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_LOCKED);
        }
        boolean isHost = room.getHostId().equals(requesterId);
        boolean isOwner = item.getMemberId().equals(requesterId);
        if (!isHost && !isOwner) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        orderItemPort.delete(item);
    }

    public SettlementResult confirmOrder(String roomId, String hostId) {
        Room room = findRoomOrThrow(roomId);
        if (!room.getHostId().equals(hostId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_HOST);
        }
        if (room.getStatus() != RoomStatus.CLOSED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CONFIRMABLE);
        }
        List<RoomParticipant> participants = roomParticipantPort.findByRoomId(roomId);
        if (participants.size() < 2) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CONFIRMABLE);
        }
        for (RoomParticipant p : participants) {
            if (!orderItemPort.existsByRoomIdAndMemberId(roomId, p.getMemberId())) {
                throw new BusinessException(ErrorCode.ORDER_NOT_CONFIRMABLE);
            }
        }

        int participantCount = participants.size();
        int perPerson = deliveryFeeCalculator.calculatePerPersonFee(room.getDeliveryFee(), participantCount);
        int surplus = deliveryFeeCalculator.calculateHostSurplus(room.getDeliveryFee(), participantCount);

        List<OrderItem> allItems = orderItemPort.findByRoomId(roomId);
        int totalMenuAmount = allItems.stream()
                .mapToInt(i -> i.getPrice() * i.getQuantity())
                .sum();

        Settlement settlement = settlementPort.save(Settlement.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .totalMenuAmount(totalMenuAmount)
                .totalDeliveryFee(room.getDeliveryFee())
                .participantCount(participantCount)
                .deliveryFeePerPerson(perPerson)
                .hostSurplus(surplus)
                .createdAt(LocalDateTime.now())
                .build());

        List<MemberSettlement> memberSettlements = new ArrayList<>();
        for (RoomParticipant p : participants) {
            int menuAmount = orderItemPort.sumAmountByRoomIdAndMemberId(roomId, p.getMemberId());
            memberSettlements.add(MemberSettlement.builder()
                    .id(UUID.randomUUID().toString())
                    .settlementId(settlement.getId())
                    .memberId(p.getMemberId())
                    .menuAmount(menuAmount)
                    .deliveryFeeShare(perPerson)
                    .totalAmount(menuAmount + perPerson)
                    .isHost(room.getHostId().equals(p.getMemberId()))
                    .build());
        }
        memberSettlementPort.saveAll(memberSettlements);

        roomService.transitionToConfirmed(roomId);
        notificationPort.sendToRoom(roomId, "주문이 확정되었습니다", "정산 금액을 확인해주세요.");

        return new SettlementResult(settlement, memberSettlements, room);
    }

    public SettlementResult getSettlement(String roomId, String memberId) {
        if (!roomParticipantPort.existsByRoomIdAndMemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Settlement settlement = settlementPort.findByRoomId(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_CONFIRMABLE));
        List<MemberSettlement> memberSettlements = memberSettlementPort.findBySettlementId(settlement.getId());
        Room room = findRoomOrThrow(roomId);
        return new SettlementResult(settlement, memberSettlements, room);
    }

    public List<OrderItem> getOrderItems(String roomId, String memberId) {
        if (memberId != null) {
            return orderItemPort.findByRoomIdAndMemberId(roomId, memberId);
        }
        return orderItemPort.findByRoomId(roomId);
    }

    private Room findRoomOrThrow(String roomId) {
        return roomPort.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    public record SettlementResult(
            Settlement settlement,
            List<MemberSettlement> memberSettlements,
            Room room
    ) {}
}
