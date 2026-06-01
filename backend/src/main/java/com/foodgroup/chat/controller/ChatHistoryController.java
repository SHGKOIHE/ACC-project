package com.foodgroup.chat.controller;

import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.service.ChatService;
import com.foodgroup.common.dto.ApiResponse;
import com.foodgroup.common.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatService chatService;

    @GetMapping("/{roomId}/chats")
    public ApiResponse<List<ChatMessageResponse>> getHistory(
            @PathVariable String roomId,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ApiResponse.ok(chatService.getHistory(roomId, principal.memberId()));
    }
}
