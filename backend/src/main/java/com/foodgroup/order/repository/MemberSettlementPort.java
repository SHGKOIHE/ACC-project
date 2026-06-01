package com.foodgroup.order.repository;

import com.foodgroup.order.domain.MemberSettlement;

import java.util.List;

public interface MemberSettlementPort {
    List<MemberSettlement> saveAll(List<MemberSettlement> settlements);
    List<MemberSettlement> findBySettlementId(String settlementId);
}
