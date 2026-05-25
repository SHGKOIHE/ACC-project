package com.foodgroup.order.repository;

import com.foodgroup.order.domain.MemberSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberSettlementRepository extends JpaRepository<MemberSettlement, Long> {
    List<MemberSettlement> findBySettlementId(Long settlementId);
}
