package com.foodgroup.auth.service;

import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.common.notification.NotificationPort;
import com.foodgroup.order.repository.OrderItemPort;
import com.foodgroup.room.domain.MeetingType;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.domain.RoomParticipant;
import com.foodgroup.room.domain.RoomStatus;
import com.foodgroup.room.repository.RoomParticipantPort;
import com.foodgroup.room.repository.RoomPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawServiceTest {

    @InjectMocks WithdrawService withdrawService;
    @Mock MemberPort memberPort;
    @Mock RoomPort roomPort;
    @Mock RoomParticipantPort roomParticipantPort;
    @Mock OrderItemPort orderItemPort;
    @Mock NotificationPort notificationPort;

    @Test
    void withdraw_방장이면_방_취소후_탈퇴() {
        Room room = buildRoom("room-1", "host-1", RoomStatus.OPEN);
        RoomParticipant hostParticipation = RoomParticipant.builder()
                .id("room-1#host-1").roomId("room-1").memberId("host-1").build();
        RoomParticipant otherParticipation = RoomParticipant.builder()
                .id("room-1#member-2").roomId("room-1").memberId("member-2").build();

        given(roomParticipantPort.findByMemberId("host-1")).willReturn(List.of(hostParticipation));
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));
        given(roomParticipantPort.findByRoomId("room-1")).willReturn(List.of(hostParticipation, otherParticipation));
        given(roomPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        withdrawService.withdraw("host-1");

        verify(notificationPort).sendToRoom(eq("room-1"), any(), any());
        verify(orderItemPort).deleteByRoomIdAndMemberId("room-1", "host-1");
        verify(orderItemPort).deleteByRoomIdAndMemberId("room-1", "member-2");
        verify(memberPort).deleteById("host-1");
    }

    @Test
    void withdraw_일반참여자면_주문취소_후_탈퇴() {
        Room room = buildRoom("room-1", "host-1", RoomStatus.OPEN);
        RoomParticipant participation = RoomParticipant.builder()
                .id("room-1#member-2").roomId("room-1").memberId("member-2").build();

        given(roomParticipantPort.findByMemberId("member-2")).willReturn(List.of(participation));
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));
        given(roomPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        withdrawService.withdraw("member-2");

        verify(orderItemPort).deleteByRoomIdAndMemberId("room-1", "member-2");
        verify(roomParticipantPort).delete(participation);
        verify(memberPort).deleteById("member-2");
        verify(notificationPort, never()).sendToRoom(any(), any(), any());
    }

    @Test
    void withdraw_참여중인_방_없으면_그냥_탈퇴() {
        given(roomParticipantPort.findByMemberId("member-9")).willReturn(List.of());

        withdrawService.withdraw("member-9");

        verify(memberPort).deleteById("member-9");
        verify(roomPort, never()).findById(any());
    }

    @Test
    void withdraw_완료된방은_건드리지_않음() {
        Room room = buildRoom("room-1", "host-1", RoomStatus.COMPLETED);
        RoomParticipant participation = RoomParticipant.builder()
                .id("room-1#member-2").roomId("room-1").memberId("member-2").build();

        given(roomParticipantPort.findByMemberId("member-2")).willReturn(List.of(participation));
        given(roomPort.findById("room-1")).willReturn(Optional.of(room));

        withdrawService.withdraw("member-2");

        verify(orderItemPort, never()).deleteByRoomIdAndMemberId(any(), any());
        verify(roomParticipantPort).delete(participation);
        verify(memberPort).deleteById("member-2");
    }

    private Room buildRoom(String id, String hostId, RoomStatus status) {
        return Room.builder()
                .id(id).hostId(hostId).title("테스트")
                .meetingType(MeetingType.DELIVERY)
                .restaurantName("식당").restaurantAddress("서울")
                .latitude(37.5).longitude(127.0)
                .deliveryFee(3000).maxParticipants(4).currentParticipantCount(2)
                .status(status).build();
    }
}
