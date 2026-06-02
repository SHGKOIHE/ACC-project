package com.foodgroup.chat.service;

import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.chat.domain.ChatMessage;
import com.foodgroup.chat.domain.ChatMessageType;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.repository.ChatMessagePort;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.room.repository.RoomParticipantPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessagePort chatMessagePort;
    private final MemberPort memberPort;
    private final RoomParticipantPort roomParticipantPort;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageResponse saveAndBroadcast(String roomId, String memberId, ChatMessageType type, String content) {
        validateMessage(roomId, memberId, type, content);

        ChatMessage saved = chatMessagePort.save(roomId, memberId, type, content);

        String nickname = memberId != null
                ? memberPort.findById(memberId).map(m -> m.getNickname()).orElse("알 수 없음")
                : null;

        ChatMessageResponse response = ChatMessageResponse.of(saved, nickname);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);
        return response;
    }

    private void validateMessage(String roomId, String memberId, ChatMessageType type, String content) {
        if (type == null || content == null || content.isBlank() || content.length() > 1000) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (memberId != null && !roomParticipantPort.existsByRoomIdAndMemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_PARTICIPANT);
        }
        if (memberId == null && type != ChatMessageType.NOTICE) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    public List<ChatMessageResponse> getHistory(String roomId, String memberId) {
        if (!roomParticipantPort.existsByRoomIdAndMemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        List<ChatMessage> messages = chatMessagePort.findTop50ByRoomId(roomId);
        return messages.stream()
                .map(m -> {
                    String nickname = m.getMemberId() != null
                            ? memberPort.findById(m.getMemberId()).map(member -> member.getNickname()).orElse("알 수 없음")
                            : null;
                    return ChatMessageResponse.of(m, nickname);
                })
                .toList();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        chatMessagePort.deleteOldMessages(cutoff);
        log.info("DynamoDB TTL handles chat message expiry (no-op trigger logged)");
    }
}
