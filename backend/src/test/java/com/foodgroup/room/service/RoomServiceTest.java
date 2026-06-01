package com.foodgroup.room.service;

import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.common.notification.NotificationPort;
import com.foodgroup.order.repository.OrderItemPort;
import com.foodgroup.room.domain.MeetingType;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomStatus;
import com.foodgroup.room.repository.RoomParticipantPort;
import com.foodgroup.room.repository.RoomPort;
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
class RoomServiceTest {

    @InjectMocks RoomService roomService;
    @Mock RoomPort roomPort;
    @Mock RoomParticipantPort roomParticipantPort;
    @Mock OrderItemPort orderItemPort;
    @Spy RoomStateValidator stateValidator;
    @Mock NotificationPort notificationPort;

    @Test
    void joinRoom_방_만원_예외() {
        Room room = buildRoom(RoomStatus.OPEN, "host-1", 2, 2);
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));
        given(roomParticipantPort.existsByRoomIdAndMemberId("room-1", "member-2")).willReturn(false);

        assertThatThrownBy(() -> roomService.joinRoom("room-1", "member-2"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_FULL);
    }

    @Test
    void joinRoom_이미_참여_예외() {
        Room room = buildRoom(RoomStatus.OPEN, "host-1", 3, 1);
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));
        given(roomParticipantPort.existsByRoomIdAndMemberId("room-1", "member-2")).willReturn(true);

        assertThatThrownBy(() -> roomService.joinRoom("room-1", "member-2"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_JOINED);
    }

    @Test
    void joinRoom_마감후_예외() {
        Room room = buildRoom(RoomStatus.CLOSED, "host-1", 3, 1);
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.joinRoom("room-1", "member-2"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_STATUS_INVALID);
    }

    @Test
    void leaveRoom_방장_탈퇴_예외() {
        Room room = buildRoom(RoomStatus.OPEN, "host-1", 3, 2);
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.leaveRoom("room-1", "host-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.HOST_CANNOT_LEAVE);
    }

    @Test
    void leaveRoom_마감후_비방장_예외() {
        Room room = buildRoom(RoomStatus.CLOSED, "host-1", 3, 2);
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.leaveRoom("room-1", "member-2"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_STATUS_INVALID);
    }

    @Test
    void cancelRoom_완료방_예외() {
        Room room = buildRoom(RoomStatus.COMPLETED, "host-1", 3, 2);
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.cancelRoom("room-1", "host-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_COMPLETED);
    }

    @Test
    void cancelRoom_비방장_예외() {
        Room room = buildRoom(RoomStatus.OPEN, "host-1", 3, 1);
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.cancelRoom("room-1", "member-2"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_HOST);
    }

    @Test
    void closeRoom_정상() {
        Room room = buildRoom(RoomStatus.OPEN, "host-1", 3, 1);
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));
        given(orderItemPort.existsByRoomId("room-1")).willReturn(true);
        given(roomPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        roomService.closeRoom("room-1", "host-1");

        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
    }

    private Room buildRoom(RoomStatus status, String hostId, int max, int current) {
        return Room.builder()
                .id("room-1").hostId(hostId).title("테스트방")
                .meetingType(MeetingType.DELIVERY)
                .restaurantName("테스트식당").restaurantAddress("서울시")
                .latitude(37.5).longitude(127.0)
                .deliveryFee(3000).maxParticipants(max).currentParticipantCount(current)
                .status(status).build();
    }
}
