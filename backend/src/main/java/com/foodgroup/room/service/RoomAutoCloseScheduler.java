package com.foodgroup.room.service;

import com.foodgroup.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomAutoCloseScheduler {

    private final RoomRepository roomRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void closeExpiredRooms() {
        int closed = roomRepository.closeExpiredRooms(LocalDateTime.now());
        if (closed > 0) {
            log.info("자동 마감 처리: {}개", closed);
        }
    }
}
