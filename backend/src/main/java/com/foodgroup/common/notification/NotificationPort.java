package com.foodgroup.common.notification;

public interface NotificationPort {
    void sendToRoom(String roomId, String title, String body);
    void sendToMember(String memberId, String title, String body);
}
