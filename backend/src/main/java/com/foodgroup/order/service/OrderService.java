package com.foodgroup.order.service;

import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.common.notification.NotificationPort;
import com.foodgroup.order.domain.MemberSettlement;
import com.foodgroup.order.domain.OrderItem;
import com.foodgroup.order.domain.Settlement;
import com.foodgroup.order.repository.MemberSettlementRepository;
import com.foodgroup.order.repository.OrderItemRepository;
import com.foodgroup.order.repository.SettlementRepository;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomParticipant;
import com.foodgroup.room.domain.RoomStatus;
import com.foodgroup.room.repository.RoomParticipantRepository;
import com.foodgroup.room.repository.RoomRepository;
import com.foodgroup.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderItemRepository orderItemRepository;
    private final SettlementRepository settlementRepository;
    private final MemberSettlementRepository memberSettlementRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final DeliveryFeeCalculator deliveryFeeCalculator;
    private final RoomService roomService;
    private final NotificationPort notificationPort;

    @Transactional
    public OrderItem addOrderItem(Long roomId, Long memberId,
                                   String menuName, int quantity, int price) {
        Room room = findRoomOrThrow(roomId);
        if (room.getStatus() == RoomStatus.CONFIRMED
                || room.getStatus() == RoomStatus.COMPLETED
                || room.getStatus() == RoomStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_LOCKED);
        }
        if (!roomParticipantRepository.existsByRoomIdAndMemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_PARTICIPANT);
        }
        return orderItemRepository.save(OrderItem.builder()
                .roomId(roomId).memberId(memberId)
                .menuName(menuName).quantity(quantity).price(price)
                .build());
    }

    @Transactional
    public void deleteOrderItem(Long roomId, Long orderItemId, Long requesterId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
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
        orderItemRepository.delete(item);
    }

    @Transactional
    public SettlementResult confirmOrder(Long roomId, Long hostId) {
        Room room = findRoomOrThrow(roomId);
        if (!room.getHostId().equals(hostId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_HOST);
        }
        if (room.getStatus() != RoomStatus.CLOSED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CONFIRMABLE);
        }
        List<RoomParticipant> participants = roomParticipantRepository.findByRoomId(roomId);
        if (participants.size() < 2) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CONFIRMABLE);
        }
        for (RoomParticipant p : participants) {
            if (!orderItemRepository.existsByRoomIdAndMemberId(roomId, p.getMemberId())) {
                throw new BusinessException(ErrorCode.ORDER_NOT_CONFIRMABLE);
            }
        }

        int participantCount = participants.size();
        int perPerson = deliveryFeeCalculator.calculatePerPersonFee(room.getDeliveryFee(), participantCount);
        int surplus = deliveryFeeCalculator.calculateHostSurplus(room.getDeliveryFee(), participantCount);

        List<OrderItem> allItems = orderItemRepository.findByRoomId(roomId);
        int totalMenuAmount = allItems.stream()
                .mapToInt(i -> i.getPrice() * i.getQuantity())
                .sum();

        Settlement settlement = settlementRepository.save(Settlement.builder()
                .roomId(roomId)
                .totalMenuAmount(totalMenuAmount)
                .totalDeliveryFee(room.getDeliveryFee())
                .participantCount(participantCount)
                .deliveryFeePerPerson(perPerson)
                .hostSurplus(surplus)
                .build());

        List<MemberSettlement> memberSettlements = new ArrayList<>();
        for (RoomParticipant p : participants) {
            Integer menuAmount = orderItemRepository.sumAmountByRoomIdAndMemberId(roomId, p.getMemberId());
            if (menuAmount == null) menuAmount = 0;
            memberSettlements.add(MemberSettlement.builder()
                    .settlementId(settlement.getId())
                    .memberId(p.getMemberId())
                    .menuAmount(menuAmount)
                    .deliveryFeeShare(perPerson)
                    .totalAmount(menuAmount + perPerson)
                    .isHost(room.getHostId().equals(p.getMemberId()))
                    .build());
        }
        memberSettlementRepository.saveAll(memberSettlements);

        roomService.transitionToConfirmed(roomId);
        notificationPort.sendToRoom(roomId, "주문이 확정되었습니다", "정산 금액을 확인해주세요.");

        return new SettlementResult(settlement, memberSettlements, room);
    }

    @Transactional(readOnly = true)
    public SettlementResult getSettlement(Long roomId, Long memberId) {
        if (!roomParticipantRepository.existsByRoomIdAndMemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Settlement settlement = settlementRepository.findByRoomId(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_CONFIRMABLE));
        List<MemberSettlement> memberSettlements = memberSettlementRepository.findBySettlementId(settlement.getId());
        Room room = findRoomOrThrow(roomId);
        return new SettlementResult(settlement, memberSettlements, room);
    }

    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItems(Long roomId, Long memberId) {
        if (memberId != null) {
            return orderItemRepository.findByRoomIdAndMemberId(roomId, memberId);
        }
        return orderItemRepository.findByRoomId(roomId);
    }

    private Room findRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    public record SettlementResult(
            Settlement settlement,
            List<MemberSettlement> memberSettlements,
            Room room
    ) {}
}
