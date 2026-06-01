package com.foodgroup.order.repository;

import com.foodgroup.order.domain.Settlement;

import java.util.Optional;

public interface SettlementPort {
    Settlement save(Settlement settlement);
    Optional<Settlement> findById(String id);
    Optional<Settlement> findByRoomId(String roomId);
}
