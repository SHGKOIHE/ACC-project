package com.foodgroup.order.repository;

import com.foodgroup.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByRoomId(Long roomId);

    List<OrderItem> findByRoomIdAndMemberId(Long roomId, Long memberId);

    boolean existsByRoomId(Long roomId);

    boolean existsByRoomIdAndMemberId(Long roomId, Long memberId);

    @Modifying
    void deleteByRoomIdAndMemberId(Long roomId, Long memberId);

    @Query("SELECT SUM(oi.price * oi.quantity) FROM OrderItem oi WHERE oi.roomId = :roomId AND oi.memberId = :memberId")
    Integer sumAmountByRoomIdAndMemberId(@Param("roomId") Long roomId, @Param("memberId") Long memberId);
}
