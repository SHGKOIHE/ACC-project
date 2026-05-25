package com.foodgroup.room.service;

import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.common.notification.NotificationPort;
import com.foodgroup.order.repository.OrderItemRepository;
import com.foodgroup.room.domain.MeetingType;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomStatus;
import com.foodgroup.room.repository.RoomParticipantRepository;
import com.foodgroup.room.repository.RoomRepository;
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
    @Mock RoomRepository roomRepository;
    @Mock RoomParticipantRepository roomParticipantRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Spy RoomStateValidator stateValidator;
    @Mock NotificationPort notificationPort;

    @Test
    void joinRoom_방_만원_예외() {
        Room room = buildRoom(RoomStatus.OPEN, 1L, 2, 2);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomParticipantRepository.existsByRoomIdAndMemberId(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> roomService.joinRoom(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_FULL);
    }

    @Test
    void joinRoom_이미_참여_예외() {
        Room room = buildRoom(RoomStatus.OPEN, 1L, 3, 1);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomParticipantRepository.existsByRoomIdAndMemberId(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> roomService.joinRoom(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_JOINED);
    }

    @Test
    void joinRoom_마감후_예외() {
        Room room = buildRoom(RoomStatus.CLOSED, 1L, 3, 1);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.joinRoom(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_STATUS_INVALID);
    }

    @Test
    void leaveRoom_방장_탈퇴_예외() {
        Room room = buildRoom(RoomStatus.OPEN, 1L, 3, 2);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.leaveRoom(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.HOST_CANNOT_LEAVE);
    }

    @Test
    void leaveRoom_마감후_비방장_예외() {
        Room room = buildRoom(RoomStatus.CLOSED, 1L, 3, 2);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.leaveRoom(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_STATUS_INVALID);
    }

    @Test
    void cancelRoom_완료방_예외() {
        Room room = buildRoom(RoomStatus.COMPLETED, 1L, 3, 2);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.cancelRoom(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_COMPLETED);
    }

    @Test
    void cancelRoom_비방장_예외() {
        Room room = buildRoom(RoomStatus.OPEN, 1L, 3, 1);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.cancelRoom(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_HOST);
    }

    @Test
    void closeRoom_정상() {
        Room room = buildRoom(RoomStatus.OPEN, 1L, 3, 1);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(orderItemRepository.existsByRoomId(1L)).willReturn(true);

        roomService.closeRoom(1L, 1L);

        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
    }

    private Room buildRoom(RoomStatus status, Long hostId, int max, int current) {
        return Room.builder()
                .id(1L).hostId(hostId).title("테스트방")
                .meetingType(MeetingType.DELIVERY)
                .restaurantName("테스트식당").restaurantAddress("서울시")
                .latitude(37.5).longitude(127.0)
                .deliveryFee(3000).maxParticipants(max).currentParticipantCount(current)
                .status(status).build();
    }
}
