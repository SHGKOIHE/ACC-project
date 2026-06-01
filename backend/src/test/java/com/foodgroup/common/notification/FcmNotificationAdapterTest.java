package com.foodgroup.common.notification;

import com.foodgroup.auth.domain.Member;
import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.room.domain.RoomParticipant;
import com.foodgroup.room.repository.RoomParticipantPort;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FcmNotificationAdapterTest {

    @InjectMocks FcmNotificationAdapter adapter;
    @Mock FirebaseMessaging firebaseMessaging;
    @Mock MemberPort memberPort;
    @Mock RoomParticipantPort roomParticipantPort;

    @Test
    void sendToMember_sends_when_fcm_token_present() throws Exception {
        Member member = Member.builder().id("m-1").nickname("짱구").deviceToken("d").fcmToken("fcm-token-123").build();
        given(memberPort.findById("m-1")).willReturn(Optional.of(member));
        given(firebaseMessaging.send(any(Message.class))).willReturn("msg-id");

        adapter.sendToMember("m-1", "제목", "내용");

        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void sendToMember_skips_when_fcm_token_null() throws Exception {
        Member member = Member.builder().id("m-1").nickname("짱구").deviceToken("d").fcmToken(null).build();
        given(memberPort.findById("m-1")).willReturn(Optional.of(member));

        adapter.sendToMember("m-1", "제목", "내용");

        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void sendToMember_skips_when_member_not_found() throws Exception {
        given(memberPort.findById("m-99")).willReturn(Optional.empty());

        adapter.sendToMember("m-99", "제목", "내용");

        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void sendToMember_logs_warning_on_firebase_exception() throws Exception {
        Member member = Member.builder().id("m-1").nickname("짱구").deviceToken("d").fcmToken("bad-token").build();
        given(memberPort.findById("m-1")).willReturn(Optional.of(member));
        given(firebaseMessaging.send(any(Message.class))).willThrow(new RuntimeException("FCM error"));

        adapter.sendToMember("m-1", "제목", "내용");
    }

    @Test
    void sendToRoom_sends_to_all_participants() throws Exception {
        RoomParticipant p1 = RoomParticipant.builder().id("r1#m10").roomId("room-1").memberId("m-10").build();
        RoomParticipant p2 = RoomParticipant.builder().id("r1#m20").roomId("room-1").memberId("m-20").build();
        Member m1 = Member.builder().id("m-10").nickname("a").deviceToken("d1").fcmToken("t1").build();
        Member m2 = Member.builder().id("m-20").nickname("b").deviceToken("d2").fcmToken("t2").build();

        given(roomParticipantPort.findByRoomId("room-1")).willReturn(List.of(p1, p2));
        given(memberPort.findById("m-10")).willReturn(Optional.of(m1));
        given(memberPort.findById("m-20")).willReturn(Optional.of(m2));
        given(firebaseMessaging.send(any(Message.class))).willReturn("msg-id");

        adapter.sendToRoom("room-1", "제목", "내용");

        verify(firebaseMessaging, times(2)).send(any(Message.class));
    }
}
