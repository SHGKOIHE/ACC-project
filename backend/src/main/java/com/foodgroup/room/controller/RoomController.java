package com.foodgroup.room.controller;

import com.foodgroup.common.dto.ApiResponse;
import com.foodgroup.common.security.MemberPrincipal;
import com.foodgroup.room.domain.MeetingType;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Room", description = "방 API")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "방 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoomResponse> create(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody CreateRoomRequest request) {
        Room room = roomService.createRoom(request.toCommand(principal.memberId()));
        return ApiResponse.ok(RoomResponse.from(room));
    }

    @Operation(summary = "방 목록 조회")
    @GetMapping
    public ApiResponse<List<RoomResponse>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) MeetingType meetingType,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius) {
        return ApiResponse.ok(
                roomService.searchRooms(category, meetingType, lat, lng, radius)
                        .stream().map(RoomResponse::from).toList());
    }

    @Operation(summary = "방 상세 조회")
    @GetMapping("/{id}")
    public ApiResponse<RoomResponse> get(@PathVariable Long id,
                                         @AuthenticationPrincipal MemberPrincipal principal) {
        Room room = roomService.getRoom(id);
        boolean isParticipant = roomService.isParticipant(id, principal.memberId());
        return ApiResponse.ok(RoomResponse.from(room, isParticipant));
    }

    @Operation(summary = "방 참여")
    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void join(@PathVariable Long id,
                     @AuthenticationPrincipal MemberPrincipal principal) {
        roomService.joinRoom(id, principal.memberId());
    }

    @Operation(summary = "방 탈퇴")
    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long id,
                      @AuthenticationPrincipal MemberPrincipal principal) {
        roomService.leaveRoom(id, principal.memberId());
    }

    @Operation(summary = "방 마감 (방장)")
    @PostMapping("/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable Long id,
                      @AuthenticationPrincipal MemberPrincipal principal) {
        roomService.closeRoom(id, principal.memberId());
    }

    @Operation(summary = "방 취소 (방장)")
    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id,
                       @AuthenticationPrincipal MemberPrincipal principal) {
        roomService.cancelRoom(id, principal.memberId());
    }

    @Operation(summary = "완료 처리 (방장)")
    @PostMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@PathVariable Long id,
                         @AuthenticationPrincipal MemberPrincipal principal) {
        roomService.completeRoom(id, principal.memberId());
    }

    // --- inner records ---

    public record CreateRoomRequest(
            @NotBlank @Size(min = 2, max = 30) String title,
            @NotNull MeetingType meetingType,
            @NotBlank String restaurantName,
            @NotBlank String restaurantAddress,
            String restaurantCategory,
            @NotNull Double latitude,
            @NotNull Double longitude,
            @NotNull @Min(0) Integer deliveryFee,
            @NotNull @Min(2) @Max(20) Integer maxParticipants,
            LocalDateTime closedAt,
            String meetingAddress,
            String accountNumber,
            String accountHolder,
            String bankName
    ) {
        public RoomService.CreateRoomCommand toCommand(Long hostId) {
            return new RoomService.CreateRoomCommand(
                    hostId, title, meetingType, restaurantName, restaurantAddress,
                    restaurantCategory, latitude, longitude, deliveryFee, maxParticipants,
                    closedAt, meetingAddress, accountNumber, accountHolder, bankName);
        }
    }

    public record RoomResponse(
            Long id, Long hostId, String title, String meetingType,
            String restaurantName, String restaurantAddress, String restaurantCategory,
            Double latitude, Double longitude, Integer deliveryFee,
            Integer maxParticipants, Integer currentParticipantCount,
            String status, LocalDateTime closedAt, String meetingAddress,
            String accountHolder, String bankName, LocalDateTime createdAt,
            boolean isParticipant
    ) {
        public static RoomResponse from(Room r, boolean isParticipant) {
            return new RoomResponse(
                    r.getId(), r.getHostId(), r.getTitle(), r.getMeetingType().name(),
                    r.getRestaurantName(), r.getRestaurantAddress(), r.getRestaurantCategory(),
                    r.getLatitude(), r.getLongitude(), r.getDeliveryFee(),
                    r.getMaxParticipants(), r.getCurrentParticipantCount(),
                    r.getStatus().name(), r.getClosedAt(), r.getMeetingAddress(),
                    r.getAccountHolder(), r.getBankName(), r.getCreatedAt(),
                    isParticipant);
        }

        public static RoomResponse from(Room r) {
            return from(r, false);
        }
    }
}
