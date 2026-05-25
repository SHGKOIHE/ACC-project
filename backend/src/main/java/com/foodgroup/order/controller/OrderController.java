package com.foodgroup.order.controller;

import com.foodgroup.common.dto.ApiResponse;
import com.foodgroup.common.security.MemberPrincipal;
import com.foodgroup.order.domain.MemberSettlement;
import com.foodgroup.order.domain.OrderItem;
import com.foodgroup.order.domain.Settlement;
import com.foodgroup.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/api/rooms/{roomId}/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 항목 추가")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderItemResponse> add(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody AddOrderItemRequest request) {
        OrderItem item = orderService.addOrderItem(
                roomId, principal.memberId(),
                request.menuName(), request.quantity(), request.price());
        return ApiResponse.ok(OrderItemResponse.from(item));
    }

    @Operation(summary = "주문 항목 목록")
    @GetMapping
    public ApiResponse<List<OrderItemResponse>> list(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(required = false) Boolean mine) {
        Long memberId = Boolean.TRUE.equals(mine) ? principal.memberId() : null;
        return ApiResponse.ok(orderService.getOrderItems(roomId, memberId)
                .stream().map(OrderItemResponse::from).toList());
    }

    @Operation(summary = "주문 항목 삭제")
    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long roomId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal MemberPrincipal principal) {
        orderService.deleteOrderItem(roomId, itemId, principal.memberId());
    }

    @Operation(summary = "주문 확정 (방장)")
    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SettlementResponse> confirm(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberPrincipal principal) {
        OrderService.SettlementResult result = orderService.confirmOrder(roomId, principal.memberId());
        return ApiResponse.ok(SettlementResponse.from(result));
    }

    @Operation(summary = "정산 조회")
    @GetMapping("/settlement")
    public ApiResponse<SettlementResponse> settlement(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ApiResponse.ok(SettlementResponse.from(orderService.getSettlement(roomId, principal.memberId())));
    }

    // --- inner records ---

    public record AddOrderItemRequest(
            @NotBlank @Size(min = 1, max = 50) String menuName,
            @NotNull @Min(1) Integer quantity,
            @NotNull @Min(0) Integer price
    ) {}

    public record OrderItemResponse(Long id, Long memberId, String menuName, int quantity, int price) {
        public static OrderItemResponse from(OrderItem i) {
            return new OrderItemResponse(i.getId(), i.getMemberId(), i.getMenuName(), i.getQuantity(), i.getPrice());
        }
    }

    public record SettlementResponse(
            Long settlementId, int totalMenuAmount, int totalDeliveryFee,
            int participantCount, int deliveryFeePerPerson, int hostSurplus,
            List<MemberSettlementResponse> members,
            String bankName, String accountHolder, String accountNumber
    ) {
        public static SettlementResponse from(OrderService.SettlementResult result) {
            Settlement s = result.settlement();
            return new SettlementResponse(
                    s.getId(), s.getTotalMenuAmount(), s.getTotalDeliveryFee(),
                    s.getParticipantCount(), s.getDeliveryFeePerPerson(), s.getHostSurplus(),
                    result.memberSettlements().stream().map(MemberSettlementResponse::from).toList(),
                    result.room().getBankName(), result.room().getAccountHolder(),
                    result.room().getAccountNumber());
        }
    }

    public record MemberSettlementResponse(
            Long memberId, int menuAmount, int deliveryFeeShare, int totalAmount, boolean isHost
    ) {
        public static MemberSettlementResponse from(MemberSettlement ms) {
            return new MemberSettlementResponse(
                    ms.getMemberId(), ms.getMenuAmount(),
                    ms.getDeliveryFeeShare(), ms.getTotalAmount(), ms.getIsHost());
        }
    }
}
