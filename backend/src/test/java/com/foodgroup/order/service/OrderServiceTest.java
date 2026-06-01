package com.foodgroup.order.service;

import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.common.notification.NotificationPort;
import com.foodgroup.order.domain.OrderItem;
import com.foodgroup.order.repository.MemberSettlementPort;
import com.foodgroup.order.repository.OrderItemPort;
import com.foodgroup.order.repository.SettlementPort;
import com.foodgroup.room.domain.MeetingType;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomStatus;
import com.foodgroup.room.repository.RoomParticipantPort;
import com.foodgroup.room.repository.RoomPort;
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
    @Mock OrderItemPort orderItemPort;
    @Mock SettlementPort settlementPort;
    @Mock MemberSettlementPort memberSettlementPort;
    @Mock RoomPort roomPort;
    @Mock RoomParticipantPort roomParticipantPort;
    @Spy DeliveryFeeCalculator deliveryFeeCalculator;
    @Mock RoomService roomService;
    @Mock NotificationPort notificationPort;

    @Test
    void addOrderItem_확정후_예외() {
        given(roomPort.findById("room-1")).willReturn(Optional.of(buildRoom(RoomStatus.CONFIRMED, "host-1")));

        assertThatThrownBy(() -> orderService.addOrderItem("room-1", "member-2", "김치찌개", 1, 8000))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_LOCKED);
    }

    @Test
    void addOrderItem_비참여자_예외() {
        given(roomPort.findById("room-1")).willReturn(Optional.of(buildRoom(RoomStatus.OPEN, "host-1")));
        given(roomParticipantPort.existsByRoomIdAndMemberId("room-1", "member-2")).willReturn(false);

        assertThatThrownBy(() -> orderService.addOrderItem("room-1", "member-2", "김치찌개", 1, 8000))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_PARTICIPANT);
    }

    @Test
    void deleteOrderItem_소유자아님_예외() {
        OrderItem item = OrderItem.builder().id("item-1").roomId("room-1").memberId("member-2")
                .menuName("김치찌개").quantity(1).price(8000).build();
        given(orderItemPort.findById("item-1")).willReturn(Optional.of(item));
        given(roomPort.findById("room-1")).willReturn(Optional.of(buildRoom(RoomStatus.OPEN, "host-1")));

        // memberId=member-3: not owner(member-2), not host(host-1)
        assertThatThrownBy(() -> orderService.deleteOrderItem("room-1", "item-1", "member-3"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void deleteOrderItem_확정후_예외() {
        OrderItem item = OrderItem.builder().id("item-1").roomId("room-1").memberId("member-2")
                .menuName("김치찌개").quantity(1).price(8000).build();
        given(orderItemPort.findById("item-1")).willReturn(Optional.of(item));
        given(roomPort.findById("room-1")).willReturn(Optional.of(buildRoom(RoomStatus.CONFIRMED, "host-1")));

        assertThatThrownBy(() -> orderService.deleteOrderItem("room-1", "item-1", "member-2"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_LOCKED);
    }

    @Test
    void confirmOrder_비방장_예외() {
        given(roomPort.findById("room-1")).willReturn(Optional.of(buildRoom(RoomStatus.CLOSED, "host-1")));

        assertThatThrownBy(() -> orderService.confirmOrder("room-1", "member-2"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_HOST);
    }

    @Test
    void confirmOrder_OPEN상태_예외() {
        given(roomPort.findById("room-1")).willReturn(Optional.of(buildRoom(RoomStatus.OPEN, "host-1")));

        assertThatThrownBy(() -> orderService.confirmOrder("room-1", "host-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_CONFIRMABLE);
    }

    private Room buildRoom(RoomStatus status, String hostId) {
        return Room.builder()
                .id("room-1").hostId(hostId).title("테스트")
                .meetingType(MeetingType.DELIVERY)
                .restaurantName("식당").restaurantAddress("서울")
                .latitude(37.5).longitude(127.0)
                .deliveryFee(3000).maxParticipants(4).currentParticipantCount(2)
                .status(status).build();
    }
}
