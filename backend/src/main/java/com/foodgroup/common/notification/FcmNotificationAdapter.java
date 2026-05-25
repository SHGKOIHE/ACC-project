package com.foodgroup.common.notification;

import com.foodgroup.auth.repository.MemberRepository;
import com.foodgroup.room.repository.RoomParticipantRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Primary
@ConditionalOnBean(FirebaseMessaging.class)
@RequiredArgsConstructor
public class FcmNotificationAdapter implements NotificationPort {

    private final FirebaseMessaging firebaseMessaging;
    private final MemberRepository memberRepository;
    private final RoomParticipantRepository roomParticipantRepository;

    @Async
    @Override
    public void sendToRoom(Long roomId, String title, String body) {
        List<Long> memberIds = roomParticipantRepository.findByRoomId(roomId).stream()
                .map(p -> p.getMemberId())
                .toList();
        memberIds.forEach(memberId -> sendToMember(memberId, title, body));
    }

    @Override
    public void sendToMember(Long memberId, String title, String body) {
        memberRepository.findById(memberId).ifPresent(member -> {
            if (member.getFcmToken() == null) return;
            try {
                Message message = Message.builder()
                        .setToken(member.getFcmToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build();
                firebaseMessaging.send(message);
            } catch (Exception e) {
                log.warn("FCM send failed for memberId={}: {}", memberId, e.getMessage());
            }
        });
    }
}
