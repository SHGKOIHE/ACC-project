package com.foodgroup.common.notification;

public interface NotificationPort {
    void sendToRoom(Long roomId, String title, String body);
    void sendToMember(Long memberId, String title, String body);
}
