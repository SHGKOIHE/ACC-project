package com.foodgroup.order.repository;

import com.foodgroup.order.domain.OrderItem;

import java.util.List;
import java.util.Optional;

public interface OrderItemPort {
    OrderItem save(OrderItem item);
    Optional<OrderItem> findById(String id);
    List<OrderItem> findByRoomId(String roomId);
    List<OrderItem> findByRoomIdAndMemberId(String roomId, String memberId);
    boolean existsByRoomId(String roomId);
    boolean existsByRoomIdAndMemberId(String roomId, String memberId);
    void deleteByRoomIdAndMemberId(String roomId, String memberId);
    int sumAmountByRoomIdAndMemberId(String roomId, String memberId);
    void delete(OrderItem item);
}
