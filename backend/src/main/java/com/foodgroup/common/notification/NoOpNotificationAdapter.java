package com.foodgroup.common.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoOpNotificationAdapter implements NotificationPort {

    @Override
    public void sendToRoom(Long roomId, String title, String body) {
        log.debug("NoOp notification → room {}: {}", roomId, title);
    }

    @Override
    public void sendToMember(Long memberId, String title, String body) {
        log.debug("NoOp notification → member {}: {}", memberId, title);
    }
}
