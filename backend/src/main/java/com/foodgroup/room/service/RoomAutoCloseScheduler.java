package com.foodgroup.room.service;

import com.foodgroup.common.notification.NotificationPort;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.repository.RoomPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomAutoCloseScheduler {

    private final RoomPort roomPort;
    private final NotificationPort notificationPort;

    @Scheduled(fixedDelay = 60_000)
    public void closeExpiredRooms() {
        int closed = roomPort.closeExpiredRooms(LocalDateTime.now());
        if (closed > 0) {
            log.info("자동 마감 처리: {}개", closed);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void notifyClosingSoonRooms() {
        LocalDateTime now = LocalDateTime.now();
        List<Room> rooms = roomPort.findOpenClosingBetween(
                now.plusMinutes(30), now.plusMinutes(31));
        for (Room room : rooms) {
            notificationPort.sendToRoom(room.getId(), "마감 임박",
                    room.getTitle() + " 방이 30분 후 마감됩니다.");
        }
        if (!rooms.isEmpty()) {
            log.info("마감 임박 알림 발송: {}개", rooms.size());
        }
    }
}
