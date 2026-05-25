package com.foodgroup.order.service;

import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.common.notification.NotificationPort;
import com.foodgroup.order.domain.OrderItem;
import com.foodgroup.order.repository.MemberSettlementRepository;
import com.foodgroup.order.repository.OrderItemRepository;
import com.foodgroup.order.repository.SettlementRepository;
import com.foodgroup.room.domain.MeetingType;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomStatus;
import com.foodgroup.room.repository.RoomParticipantRepository;
import com.foodgroup.room.repository.RoomRepository;
import com.foodgroup.room.service.RoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks OrderService orderService;
    @Mock OrderItemRepository orderItemRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock MemberSettlementRepository memberSettlementRepository;
    @Mock RoomRepository roomRepository;
    @Mock RoomParticipantRepository roomParticipantRepository;
    @Spy DeliveryFeeCalculator deliveryFeeCalculator;
    @Mock RoomService roomService;
    @Mock NotificationPort notificationPort;

    @Test
    void addOrderItem_확정후_예외() {
        given(roomRepository.findById(1L)).willReturn(Optional.of(buildRoom(RoomStatus.CONFIRMED, 1L)));

        assertThatThrownBy(() -> orderService.addOrderItem(1L, 2L, "김치찌개", 1, 8000))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_LOCKED);
    }

    @Test
    void addOrderItem_비참여자_예외() {
        given(roomRepository.findById(1L)).willReturn(Optional.of(buildRoom(RoomStatus.OPEN, 1L)));
        given(roomParticipantRepository.existsByRoomIdAndMemberId(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> orderService.addOrderItem(1L, 2L, "김치찌개", 1, 8000))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_PARTICIPANT);
    }

    @Test
    void deleteOrderItem_소유자아님_예외() {
        OrderItem item = OrderItem.builder().id(1L).roomId(1L).memberId(2L)
                .menuName("김치찌개").quantity(1).price(8000).build();
        given(orderItemRepository.findById(1L)).willReturn(Optional.of(item));
        given(roomRepository.findById(1L)).willReturn(Optional.of(buildRoom(RoomStatus.OPEN, 1L)));

        // memberId=3L: not owner(2L), not host(1L)
        assertThatThrownBy(() -> orderService.deleteOrderItem(1L, 1L, 3L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void deleteOrderItem_확정후_예외() {
        OrderItem item = OrderItem.builder().id(1L).roomId(1L).memberId(2L)
                .menuName("김치찌개").quantity(1).price(8000).build();
        given(orderItemRepository.findById(1L)).willReturn(Optional.of(item));
        given(roomRepository.findById(1L)).willReturn(Optional.of(buildRoom(RoomStatus.CONFIRMED, 1L)));

        assertThatThrownBy(() -> orderService.deleteOrderItem(1L, 1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_LOCKED);
    }

    @Test
    void confirmOrder_비방장_예외() {
        given(roomRepository.findById(1L)).willReturn(Optional.of(buildRoom(RoomStatus.CLOSED, 1L)));

        assertThatThrownBy(() -> orderService.confirmOrder(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_HOST);
    }

    @Test
    void confirmOrder_OPEN상태_예외() {
        given(roomRepository.findById(1L)).willReturn(Optional.of(buildRoom(RoomStatus.OPEN, 1L)));

        assertThatThrownBy(() -> orderService.confirmOrder(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_CONFIRMABLE);
    }

    private Room buildRoom(RoomStatus status, Long hostId) {
        return Room.builder()
                .id(1L).hostId(hostId).title("테스트")
                .meetingType(MeetingType.DELIVERY)
                .restaurantName("식당").restaurantAddress("서울")
                .latitude(37.5).longitude(127.0)
                .deliveryFee(3000).maxParticipants(4).currentParticipantCount(2)
                .status(status).build();
    }
}
