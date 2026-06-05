package com.foodgroup.room.service;

import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.common.notification.NotificationPort;
import com.foodgroup.order.repository.OrderItemPort;
import com.foodgroup.room.domain.MeetingType;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomStatus;
import com.foodgroup.room.domain.RoomParticipant;
import com.foodgroup.room.repository.RoomParticipantPort;
import com.foodgroup.room.repository.RoomPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
    void createRoom_이미_진행중인_방이_있으면_예외() {
        Room activeRoom = buildRoom("active-room", RoomStatus.OPEN, "host-1", 3, 1);
        given(roomParticipantPort.findByMemberId("host-1")).willReturn(List.of(participant("active-room", "host-1")));
        given(roomPort.findById("active-room")).willReturn(Optional.of(activeRoom));

        assertThatThrownBy(() -> roomService.createRoom(createCommand("host-1")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_IN_ACTIVE_ROOM);
    }

    @Test
    void joinRoom_이미_다른_진행중인_방에_참여중이면_예외() {
        Room targetRoom = buildRoom("target-room", RoomStatus.OPEN, "host-1", 3, 1);
        Room activeRoom = buildRoom("active-room", RoomStatus.CLOSED, "host-2", 3, 2);
        given(roomPort.findById("target-room")).willReturn(Optional.of(targetRoom));
        given(roomParticipantPort.existsByRoomIdAndMemberId("target-room", "member-2")).willReturn(false);
        given(roomParticipantPort.findByMemberId("member-2")).willReturn(List.of(participant("active-room", "member-2")));
        given(roomPort.findById("active-room")).willReturn(Optional.of(activeRoom));

        assertThatThrownBy(() -> roomService.joinRoom("target-room", "member-2"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_IN_ACTIVE_ROOM);
    }

    @Test
    void joinRoom_완료된_이전방은_새참여를_막지_않음() {
        Room targetRoom = buildRoom("target-room", RoomStatus.OPEN, "host-1", 3, 1);
        Room completedRoom = buildRoom("completed-room", RoomStatus.COMPLETED, "host-2", 3, 2);
        given(roomPort.findById("target-room")).willReturn(Optional.of(targetRoom));
        given(roomParticipantPort.existsByRoomIdAndMemberId("target-room", "member-2")).willReturn(false);
        given(roomParticipantPort.findByMemberId("member-2")).willReturn(List.of(participant("completed-room", "member-2")));
        given(roomPort.findById("completed-room")).willReturn(Optional.of(completedRoom));
        given(roomPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        roomService.joinRoom("target-room", "member-2");

        assertThat(targetRoom.getCurrentParticipantCount()).isEqualTo(2);
        then(roomParticipantPort).should().save(any(RoomParticipant.class));
    }

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
        given(roomParticipantPort.findByRoomId("room-1")).willReturn(List.of(
                participant("host-1"),
                participant("member-2")
        ));

        assertThatThrownBy(() -> roomService.leaveRoom("room-1", "host-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.HOST_CANNOT_LEAVE);
    }

    @Test
    void leaveRoom_방장_혼자면_참여자수_캐시가_커도_방삭제() {
        Room room = buildRoom(RoomStatus.OPEN, "host-1", 3, 2);
        RoomParticipant host = participant("host-1");
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));
        given(roomParticipantPort.findByRoomId("room-1")).willReturn(List.of(host));
        given(roomParticipantPort.findByRoomIdAndMemberId("room-1", "host-1")).willReturn(Optional.of(host));

        roomService.leaveRoom("room-1", "host-1");

        then(roomParticipantPort).should().delete(host);
        then(orderItemPort).should().deleteByRoomIdAndMemberId("room-1", "host-1");
        then(roomPort).should().delete("room-1");
    }

    @Test
    void leaveRoom_방장_혼자여도_확정후에는_방삭제_불가() {
        Room room = buildRoom(RoomStatus.CONFIRMED, "host-1", 3, 1);
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.leaveRoom("room-1", "host-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_STATUS_INVALID);
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
        given(roomParticipantPort.findByRoomId("room-1")).willReturn(List.of(participant("host-1")));
        given(orderItemPort.existsByRoomIdAndMemberId("room-1", "host-1")).willReturn(true);
        given(roomPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        roomService.closeRoom("room-1", "host-1");

        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
    }


    @Test
    void closeRoom_주문하지_않은_참여자가_있으면_예외() {
        Room room = buildRoom(RoomStatus.OPEN, "host-1", 3, 2);
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));
        given(orderItemPort.existsByRoomId("room-1")).willReturn(true);
        given(roomParticipantPort.findByRoomId("room-1")).willReturn(List.of(
                participant("host-1"),
                participant("member-2")
        ));
        given(orderItemPort.existsByRoomIdAndMemberId("room-1", "host-1")).willReturn(true);
        given(orderItemPort.existsByRoomIdAndMemberId("room-1", "member-2")).willReturn(false);

        assertThatThrownBy(() -> roomService.closeRoom("room-1", "host-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_CONFIRMABLE);
    }

    @Test
    void reopenRoom_마감방을_모집중으로_되돌리고_마감시간을_초기화() {
        Room room = buildRoom(RoomStatus.CLOSED, "host-1", 3, 2);
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));
        given(roomPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        roomService.reopenRoom("room-1", "host-1");

        assertThat(room.getStatus()).isEqualTo(RoomStatus.OPEN);
        assertThat(room.getClosedAt()).isNull();
        then(roomPort).should().save(room);
    }

    @Test
    void reopenRoom_비방장은_예외() {
        Room room = buildRoom(RoomStatus.CLOSED, "host-1", 3, 2);
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.reopenRoom("room-1", "member-2"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_HOST);
    }

    @Test
    void searchRooms_참여중인_마감방도_포함() {
        Room openRoom = buildRoom("open-room", RoomStatus.OPEN, "host-1", 3, 1);
        Room closedRoom = buildRoom("closed-room", RoomStatus.CLOSED, "host-2", 3, 2);
        given(roomPort.scanByStatus(RoomStatus.OPEN)).willReturn(List.of(openRoom));
        given(roomParticipantPort.findByMemberId("member-2")).willReturn(List.of(
                participant("closed-room", "member-2")
        ));
        given(roomPort.findById("closed-room")).willReturn(Optional.of(closedRoom));

        List<Room> rooms = roomService.searchRooms(null, null, null, null, null, "member-2");

        assertThat(rooms).extracting(Room::getId).containsExactly("open-room", "closed-room");
    }

    private RoomParticipant participant(String memberId) {
        return participant("room-1", memberId);
    }

    private RoomParticipant participant(String roomId, String memberId) {
        return RoomParticipant.builder()
                .id("participant-" + roomId + "-" + memberId)
                .roomId(roomId)
                .memberId(memberId)
                .build();
    }

    private RoomService.CreateRoomCommand createCommand(String hostId) {
        return new RoomService.CreateRoomCommand(
                hostId, "새 방", MeetingType.DELIVERY, "식당", "서울시", null,
                37.5, 127.0, 3000, 3, null, null, null, null, null);
    }

    private Room buildRoom(RoomStatus status, String hostId, int max, int current) {
        return buildRoom("room-1", status, hostId, max, current);
    }

    private Room buildRoom(String roomId, RoomStatus status, String hostId, int max, int current) {
        return Room.builder()
                .id(roomId).hostId(hostId).title("테스트방")
                .meetingType(MeetingType.DELIVERY)
                .restaurantName("테스트식당").restaurantAddress("서울시")
                .latitude(37.5).longitude(127.0)
                .deliveryFee(3000).maxParticipants(max).currentParticipantCount(current)
                .status(status).build();
    }
}
