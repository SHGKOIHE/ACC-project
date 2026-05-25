package com.foodgroup.common.notification;

import com.foodgroup.auth.domain.Member;
import com.foodgroup.auth.repository.MemberRepository;
import com.foodgroup.room.domain.RoomParticipant;
import com.foodgroup.room.repository.RoomParticipantRepository;
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
    @Mock MemberRepository memberRepository;
    @Mock RoomParticipantRepository roomParticipantRepository;

    @Test
    void sendToMember_sends_when_fcm_token_present() throws Exception {
        Member member = Member.builder().id(1L).nickname("짱구").deviceToken("d").fcmToken("fcm-token-123").build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(firebaseMessaging.send(any(Message.class))).willReturn("msg-id");

        adapter.sendToMember(1L, "제목", "내용");

        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void sendToMember_skips_when_fcm_token_null() throws Exception {
        Member member = Member.builder().id(1L).nickname("짱구").deviceToken("d").fcmToken(null).build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        adapter.sendToMember(1L, "제목", "내용");

        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void sendToMember_skips_when_member_not_found() throws Exception {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        adapter.sendToMember(99L, "제목", "내용");

        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void sendToMember_logs_warning_on_firebase_exception() throws Exception {
        Member member = Member.builder().id(1L).nickname("짱구").deviceToken("d").fcmToken("bad-token").build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(firebaseMessaging.send(any(Message.class))).willThrow(new RuntimeException("FCM error"));

        // should not throw — warning is logged
        adapter.sendToMember(1L, "제목", "내용");
    }

    @Test
    void sendToRoom_sends_to_all_participants() throws Exception {
        RoomParticipant p1 = RoomParticipant.builder().roomId(1L).memberId(10L).build();
        RoomParticipant p2 = RoomParticipant.builder().roomId(1L).memberId(20L).build();
        Member m1 = Member.builder().id(10L).nickname("a").deviceToken("d1").fcmToken("t1").build();
        Member m2 = Member.builder().id(20L).nickname("b").deviceToken("d2").fcmToken("t2").build();

        given(roomParticipantRepository.findByRoomId(1L)).willReturn(List.of(p1, p2));
        given(memberRepository.findById(10L)).willReturn(Optional.of(m1));
        given(memberRepository.findById(20L)).willReturn(Optional.of(m2));
        given(firebaseMessaging.send(any(Message.class))).willReturn("msg-id");

        adapter.sendToRoom(1L, "제목", "내용");

        verify(firebaseMessaging, times(2)).send(any(Message.class));
    }
}
