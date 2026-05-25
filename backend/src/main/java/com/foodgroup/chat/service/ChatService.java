package com.foodgroup.chat.service;

import com.foodgroup.auth.repository.MemberRepository;
import com.foodgroup.chat.domain.ChatMessage;
import com.foodgroup.chat.domain.ChatMessageType;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.repository.ChatMessageRepository;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.common.exception.ErrorCode;
import com.foodgroup.room.repository.RoomParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatMessageResponse saveAndBroadcast(Long roomId, Long memberId, ChatMessageType type, String content) {
        ChatMessage message = ChatMessage.builder()
                .roomId(roomId)
                .memberId(memberId)
                .type(type)
                .content(content)
                .build();
        ChatMessage saved = chatMessageRepository.save(message);

        String nickname = memberId != null
                ? memberRepository.findById(memberId).map(m -> m.getNickname()).orElse("알 수 없음")
                : null;

        ChatMessageResponse response = ChatMessageResponse.of(saved, nickname);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getHistory(Long roomId, Long memberId) {
        if (!roomParticipantRepository.existsByRoomIdAndMemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        List<ChatMessage> messages = chatMessageRepository.findTop50ByRoomIdOrderByCreatedAtDesc(roomId);
        return messages.stream()
                .map(m -> {
                    String nickname = m.getMemberId() != null
                            ? memberRepository.findById(m.getMemberId()).map(member -> member.getNickname()).orElse("알 수 없음")
                            : null;
                    return ChatMessageResponse.of(m, nickname);
                })
                .toList();
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteOldMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        chatMessageRepository.deleteOldMessages(cutoff);
        log.info("Deleted chat messages older than 30 days from closed rooms");
    }
}
