package com.foodgroup.chat.event;

import com.foodgroup.chat.auth.JwtChannelInterceptor;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.dto.ChatMessageType;
import com.foodgroup.chat.pubsub.RedisChatPublisher;
import com.foodgroup.chat.repository.ChatMessageStore;
import com.foodgroup.chat.repository.MemberNicknameLookup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 방 채팅 토픽 구독/연결 종료 시 입장(ENTER)/퇴장(LEAVE) 시스템 메시지를 생성한다. (BR-CHAT-03)
 * 시스템 메시지도 일반 메시지와 동일하게 저장(BR-CHAT-02) 후 Redis로 발행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionEventListener {

    private static final String SESSION_NICKNAME = "nickname";
    private static final String SESSION_ROOM_ID = "roomId";
    private static final String UNKNOWN_NICKNAME = "알 수 없음";
    private static final String ROOM_PATH_MARKER = "/room/";

    private final RedisChatPublisher redisChatPublisher;
    private final ObjectProvider<ChatMessageStore> chatMessageStoreProvider;
    private final ObjectProvider<MemberNicknameLookup> memberNicknameLookupProvider;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String roomId = extractRoomId(accessor.getDestination());
        if (roomId == null) {
            return; // 방 채팅 토픽 구독이 아니면 무시
        }
        Map<String, Object> session = accessor.getSessionAttributes();
        String memberId = memberId(session);
        if (memberId == null) {
            return;
        }
        String nickname = resolveNickname(session, memberId);
        if (session != null) {
            session.put(SESSION_ROOM_ID, roomId); // 퇴장 시점에 방을 알기 위해 기억
        }
        broadcastSystemMessage(roomId, memberId, nickname, ChatMessageType.ENTER, nickname + "님이 입장했습니다");
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> session = accessor.getSessionAttributes();
        if (session == null) {
            return;
        }
        Object roomId = session.get(SESSION_ROOM_ID);
        String memberId = memberId(session);
        if (roomId == null || memberId == null) {
            return; // 방에 입장한 적 없는 세션이면 무시
        }
        String nickname = resolveNickname(session, memberId);
        broadcastSystemMessage(String.valueOf(roomId), memberId, nickname, ChatMessageType.LEAVE, nickname + "님이 나갔습니다");
    }

    private void broadcastSystemMessage(String roomId, String memberId, String nickname,
                                        ChatMessageType type, String content) {
        try {
            ChatMessageStore store = chatMessageStoreProvider.getIfAvailable();
            ChatMessageResponse message = (store != null)
                    ? store.save(roomId, memberId, nickname, type, content)
                    : new ChatMessageResponse(UUID.randomUUID().toString(), roomId, memberId, nickname,
                            type, content, LocalDateTime.now());
            redisChatPublisher.publish(message);
        } catch (Exception e) {
            // 시스템 메시지 실패가 채팅 흐름을 막지 않도록 로그만 남긴다
            log.warn("Failed to broadcast system message ({}) for room {}", type, roomId, e);
        }
    }

    private String memberId(Map<String, Object> session) {
        Object memberId = session == null ? null : session.get(JwtChannelInterceptor.SESSION_MEMBER_ID);
        return memberId == null ? null : String.valueOf(memberId);
    }

    private String resolveNickname(Map<String, Object> session, String memberId) {
        Object cached = session == null ? null : session.get(SESSION_NICKNAME);
        if (cached != null) {
            return String.valueOf(cached);
        }
        String nickname = UNKNOWN_NICKNAME;
        MemberNicknameLookup lookup = memberNicknameLookupProvider.getIfAvailable();
        if (lookup != null) {
            nickname = lookup.findNickname(memberId).orElse(UNKNOWN_NICKNAME);
        }
        if (session != null) {
            session.put(SESSION_NICKNAME, nickname);
        }
        return nickname;
    }

    private String extractRoomId(String destination) {
        if (destination == null) {
            return null;
        }
        int idx = destination.indexOf(ROOM_PATH_MARKER);
        if (idx < 0) {
            return null;
        }
        String rest = destination.substring(idx + ROOM_PATH_MARKER.length());
        int slash = rest.indexOf('/');
        String roomId = slash < 0 ? rest : rest.substring(0, slash);
        return roomId.isBlank() ? null : roomId;
    }
}
