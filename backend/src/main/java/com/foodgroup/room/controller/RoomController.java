package com.foodgroup.room.controller;

import com.foodgroup.common.dto.ApiResponse;
import com.foodgroup.common.security.MemberPrincipal;
import com.foodgroup.room.domain.MeetingType;
import com.foodgroup.room.domain.Room;
import com.foodgroup.room.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoomResponse> create(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody CreateRoomRequest request) {
        Room room = roomService.createRoom(request.toCommand(principal.memberId()));
        return ApiResponse.ok(RoomResponse.from(room));
    }

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

    @GetMapping("/{id}")
    public ApiResponse<RoomResponse> get(@PathVariable String id,
                                         @AuthenticationPrincipal MemberPrincipal principal) {
        Room room = roomService.getRoom(id);
        boolean isParticipant = roomService.isParticipant(id, principal.memberId());
        boolean isHost = room.getHostId().equals(principal.memberId());
        return ApiResponse.ok(RoomResponse.from(room, isParticipant, isHost));
    }

    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void join(@PathVariable String id,
                     @AuthenticationPrincipal MemberPrincipal principal) {
        roomService.joinRoom(id, principal.memberId());
    }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable String id,
                      @AuthenticationPrincipal MemberPrincipal principal) {
        roomService.leaveRoom(id, principal.memberId());
    }

    @PostMapping("/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable String id,
                      @AuthenticationPrincipal MemberPrincipal principal) {
        roomService.closeRoom(id, principal.memberId());
    }

    @GetMapping("/{id}/participants")
    public ApiResponse<List<ParticipantResponse>> getParticipants(@PathVariable String id) {
        return ApiResponse.ok(
                roomService.getParticipants(id).stream()
                        .map(p -> new ParticipantResponse(p.memberId(), p.nickname(), p.isHost()))
                        .toList());
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String id,
                       @AuthenticationPrincipal MemberPrincipal principal) {
        roomService.cancelRoom(id, principal.memberId());
    }

    @PostMapping("/{id}/deliver")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deliver(@PathVariable String id,
                        @AuthenticationPrincipal MemberPrincipal principal) {
        roomService.deliverRoom(id, principal.memberId());
    }

    @PostMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@PathVariable String id,
                         @AuthenticationPrincipal MemberPrincipal principal) {
        roomService.completeRoom(id, principal.memberId());
    }

    // --- inner records ---

    public record ParticipantResponse(String memberId, String nickname, boolean isHost) {}

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
        public RoomService.CreateRoomCommand toCommand(String hostId) {
            return new RoomService.CreateRoomCommand(
                    hostId, title, meetingType, restaurantName, restaurantAddress,
                    restaurantCategory, latitude, longitude, deliveryFee, maxParticipants,
                    closedAt, meetingAddress, accountNumber, accountHolder, bankName);
        }
    }

    public record RoomResponse(
            String id, String hostId, String title, String meetingType,
            String restaurantName, String restaurantAddress, String restaurantCategory,
            Double latitude, Double longitude, Integer deliveryFee,
            Integer maxParticipants, Integer currentParticipantCount,
            String status, LocalDateTime closedAt, String meetingAddress,
            String accountHolder, String bankName, LocalDateTime createdAt,
            boolean isParticipant, boolean isHost
    ) {
        public static RoomResponse from(Room r, boolean isParticipant, boolean isHost) {
            return new RoomResponse(
                    r.getId(), r.getHostId(), r.getTitle(), r.getMeetingType().name(),
                    r.getRestaurantName(), r.getRestaurantAddress(), r.getRestaurantCategory(),
                    r.getLatitude(), r.getLongitude(), r.getDeliveryFee(),
                    r.getMaxParticipants(), r.getCurrentParticipantCount(),
                    r.getStatus().name(), r.getClosedAt(), r.getMeetingAddress(),
                    r.getAccountHolder(), r.getBankName(), r.getCreatedAt(),
                    isParticipant, isHost);
        }

        public static RoomResponse from(Room r, boolean isParticipant) {
            return from(r, isParticipant, false);
        }

        public static RoomResponse from(Room r) {
            return from(r, false, false);
        }
    }
}
