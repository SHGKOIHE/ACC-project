package com.foodgroup.order.repository;

import com.foodgroup.order.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByRoomId(Long roomId);
}
