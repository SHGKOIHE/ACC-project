package com.foodgroup.ai;

import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.common.dto.ApiResponse;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.common.security.MemberPrincipal;
import com.foodgroup.order.repository.OrderItemPort;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.repository.RoomParticipantPort;
import com.foodgroup.room.repository.RoomPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/recommend")
@RequiredArgsConstructor
public class AiRecommendController {

    private final AiRecommendClient aiRecommendClient;
    private final RoomParticipantPort roomParticipantPort;
    private final RoomPort roomPort;
    private final MemberPort memberPort;
    private final OrderItemPort orderItemPort;

    @PostMapping
    public ApiResponse<AiRecommendResponse> recommend(
            @PathVariable String roomId,
            @AuthenticationPrincipal MemberPrincipal principal) {

        if (!roomParticipantPort.existsByRoomIdAndMemberId(roomId, principal.memberId())) {
            throw new BusinessException(ErrorCode.NOT_ROOM_PARTICIPANT);
        }

        Room room = roomPort.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        List<AiRecommendRequest.ParticipantInfo> participants =
                roomParticipantPort.findByRoomId(roomId).stream()
                        .map(p -> {
                            String nickname = memberPort.findById(p.getMemberId())
                                    .map(m -> m.getNickname())
                                    .orElse("알 수 없음");

                            List<AiRecommendRequest.OrderItemInfo> orderItems =
                                    orderItemPort.findByRoomIdAndMemberId(roomId, p.getMemberId())
                                            .stream()
                                            .map(oi -> new AiRecommendRequest.OrderItemInfo(
                                                    oi.getMenuName(), oi.getPrice()))
                                            .toList();

                            return new AiRecommendRequest.ParticipantInfo(nickname, orderItems);
                        })
                        .toList();

        AiRecommendRequest request = new AiRecommendRequest(
                roomId,
                participants,
                new AiRecommendRequest.FilterInfo(
                        room.getRestaurantCategory(),
                        room.getDeliveryFee(),
                        null,
                        room.getLatitude(),
                        room.getLongitude()
                )
        );

        AiRecommendResponse response = aiRecommendClient.recommend(request);
        return ApiResponse.ok(response);
    }
}
