package com.foodgroup.chat.controller;

import com.foodgroup.chat.auth.JwtChannelInterceptor;
import com.foodgroup.chat.dto.ChatMessageRequest;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.pubsub.RedisChatPublisher;
import com.foodgroup.chat.repository.ChatMessageStore;
import com.foodgroup.chat.repository.MemberNicknameLookup;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private static final String SESSION_NICKNAME = "nickname";
    private static final String UNKNOWN_NICKNAME = "알 수 없음";

    private final RedisChatPublisher redisChatPublisher;
    // DynamoDB가 비활성화된 로컬 환경에서는 저장/조회 없이도 동작하도록 Optional 주입
    private final ObjectProvider<ChatMessageStore> chatMessageStoreProvider;
    private final ObjectProvider<MemberNicknameLookup> memberNicknameLookupProvider;

    @MessageMapping("/room/{roomId}/chat")
    public void handleChat(
            @DestinationVariable String roomId,
            @Valid @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String memberId = resolveMemberId(headerAccessor);
        String nickname = resolveNickname(headerAccessor, memberId);

        ChatMessageStore store = chatMessageStoreProvider.getIfAvailable();
        ChatMessageResponse message = (store != null)
                ? store.save(roomId, memberId, nickname, request.type(), request.content())
                : new ChatMessageResponse(
                        UUID.randomUUID().toString(), roomId, memberId, nickname,
                        request.type(), request.content(), LocalDateTime.now());

        redisChatPublisher.publish(message);
    }

    private String resolveMemberId(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> attributes = headerAccessor.getSessionAttributes();
        Object memberId = attributes == null ? null : attributes.get(JwtChannelInterceptor.SESSION_MEMBER_ID);
        return memberId == null ? null : String.valueOf(memberId);
    }

    /**
     * 닉네임을 세션에 캐싱한다. 최초 1회만 Members 테이블을 조회하고 이후에는 세션 값을 재사용한다.
     * memberId가 없으면(시스템 메시지 등) 닉네임도 없다.
     */
    private String resolveNickname(SimpMessageHeaderAccessor headerAccessor, String memberId) {
        if (memberId == null) {
            return null;
        }
        Map<String, Object> attributes = headerAccessor.getSessionAttributes();
        Object cached = attributes == null ? null : attributes.get(SESSION_NICKNAME);
        if (cached != null) {
            return String.valueOf(cached);
        }

        String nickname = UNKNOWN_NICKNAME;
        MemberNicknameLookup lookup = memberNicknameLookupProvider.getIfAvailable();
        if (lookup != null) {
            nickname = lookup.findNickname(memberId).orElse(UNKNOWN_NICKNAME);
        }
        if (attributes != null) {
            attributes.put(SESSION_NICKNAME, nickname);
        }
        return nickname;
    }
}
