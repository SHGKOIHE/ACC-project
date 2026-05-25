package com.foodgroup.ai;

import com.foodgroup.auth.repository.MemberRepository;
import com.foodgroup.common.dto.ApiResponse;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.common.security.MemberPrincipal;
import com.foodgroup.order.repository.OrderItemRepository;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.repository.RoomParticipantRepository;
import com.foodgroup.room.repository.RoomRepository;
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
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;
    private final OrderItemRepository orderItemRepository;

    @PostMapping
    public ApiResponse<AiRecommendResponse> recommend(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberPrincipal principal) {

        if (!roomParticipantRepository.existsByRoomIdAndMemberId(roomId, principal.memberId())) {
            throw new BusinessException(ErrorCode.NOT_ROOM_PARTICIPANT);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        List<AiRecommendRequest.ParticipantInfo> participants =
                roomParticipantRepository.findByRoomId(roomId).stream()
                        .map(p -> {
                            String nickname = memberRepository.findById(p.getMemberId())
                                    .map(m -> m.getNickname())
                                    .orElse("알 수 없음");

                            List<AiRecommendRequest.OrderItemInfo> orderItems =
                                    orderItemRepository.findByRoomIdAndMemberId(roomId, p.getMemberId())
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
                        room.getDeliveryFee()
                )
        );

        AiRecommendResponse response = aiRecommendClient.recommend(request).block();
        return ApiResponse.ok(response);
    }
}
