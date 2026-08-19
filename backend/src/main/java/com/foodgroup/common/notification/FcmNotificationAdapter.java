package com.foodgroup.common.notification;

import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.room.repository.RoomParticipantPort;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

// Synchronous on purpose: on Lambda, work started via @Async after the handler returns can be
// frozen mid-flight when the execution environment is paused, so notifications would silently
// never send. @ConditionalOnProperty (matching FcmConfig's own condition) instead of
// @ConditionalOnBean(FirebaseMessaging.class) avoids bean-registration-order flakiness.
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FcmNotificationAdapter implements NotificationPort {

    private final FirebaseMessaging firebaseMessaging;
    private final MemberPort memberPort;
    private final RoomParticipantPort roomParticipantPort;

    @Override
    public void sendToRoom(String roomId, String title, String body) {
        roomParticipantPort.findByRoomId(roomId).stream()
                .map(p -> p.getMemberId())
                .forEach(memberId -> sendToMember(memberId, title, body));
    }

    @Override
    public void sendToMember(String memberId, String title, String body) {
        memberPort.findById(memberId).ifPresent(member -> {
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
