package com.foodgroup.chat.service;

import com.foodgroup.auth.domain.Member;
import com.foodgroup.auth.repository.MemberPort;
import com.foodgroup.chat.domain.ChatMessage;
import com.foodgroup.chat.domain.ChatMessageType;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.repository.ChatMessagePort;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.room.repository.RoomParticipantPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks ChatService chatService;
    @Mock ChatMessagePort chatMessagePort;
    @Mock MemberPort memberPort;
    @Mock RoomParticipantPort roomParticipantPort;
    @Mock SimpMessagingTemplate messagingTemplate;

    @Test
    void saveAndBroadcast_TALK_saves_and_broadcasts() {
        ChatMessage saved = ChatMessage.builder()
                .roomId("room-1").memberId("member-10").type(ChatMessageType.TALK).content("hello").build();
        Member member = Member.builder().id("member-10").nickname("짱구").deviceToken("token").build();

        given(chatMessagePort.save("room-1", "member-10", ChatMessageType.TALK, "hello")).willReturn(saved);
        given(memberPort.findById("member-10")).willReturn(Optional.of(member));

        ChatMessageResponse response = chatService.saveAndBroadcast("room-1", "member-10", ChatMessageType.TALK, "hello");

        assertThat(response.nickname()).isEqualTo("짱구");
        assertThat(response.type()).isEqualTo(ChatMessageType.TALK);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/room-1"), any(ChatMessageResponse.class));
    }

    @Test
    void saveAndBroadcast_NOTICE_null_memberId_sets_no_nickname() {
        ChatMessage saved = ChatMessage.builder()
                .roomId("room-1").memberId(null).type(ChatMessageType.NOTICE).content("방이 마감됩니다").build();

        given(chatMessagePort.save("room-1", null, ChatMessageType.NOTICE, "방이 마감됩니다")).willReturn(saved);

        ChatMessageResponse response = chatService.saveAndBroadcast("room-1", null, ChatMessageType.NOTICE, "방이 마감됩니다");

        assertThat(response.nickname()).isNull();
        verify(memberPort, never()).findById(any());
    }

    @Test
    void getHistory_throws_UNAUTHORIZED_when_not_participant() {
        given(roomParticipantPort.existsByRoomIdAndMemberId("room-1", "member-99")).willReturn(false);

        assertThatThrownBy(() -> chatService.getHistory("room-1", "member-99"))
                .isInstanceOf(BusinessException.class);
        verify(chatMessagePort, never()).findTop50ByRoomId(any());
    }

    @Test
    void getHistory_returns_messages_for_participant() {
        ChatMessage m1 = ChatMessage.builder().roomId("room-1").memberId("member-10").type(ChatMessageType.TALK).content("a").build();
        ChatMessage m2 = ChatMessage.builder().roomId("room-1").memberId("member-10").type(ChatMessageType.TALK).content("b").build();
        Member member = Member.builder().id("member-10").nickname("짱구").deviceToken("token").build();

        given(roomParticipantPort.existsByRoomIdAndMemberId("room-1", "member-10")).willReturn(true);
        given(chatMessagePort.findTop50ByRoomId("room-1")).willReturn(List.of(m1, m2));
        given(memberPort.findById("member-10")).willReturn(Optional.of(member));

        List<ChatMessageResponse> history = chatService.getHistory("room-1", "member-10");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).nickname()).isEqualTo("짱구");
    }

    @Test
    void deleteOldMessages_calls_port() {
        chatService.deleteOldMessages();
        verify(chatMessagePort).deleteOldMessages(any());
    }

    @Test
    void saveAndBroadcast_broadcasts_to_correct_topic() {
        ChatMessage saved = ChatMessage.builder()
                .roomId("room-42").memberId("member-5").type(ChatMessageType.ENTER).content("입장").build();
        Member member = Member.builder().id("member-5").nickname("철수").deviceToken("t").build();

        given(chatMessagePort.save("room-42", "member-5", ChatMessageType.ENTER, "입장")).willReturn(saved);
        given(memberPort.findById("member-5")).willReturn(Optional.of(member));

        chatService.saveAndBroadcast("room-42", "member-5", ChatMessageType.ENTER, "입장");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), any(ChatMessageResponse.class));
        assertThat(topicCaptor.getValue()).isEqualTo("/topic/room/room-42");
    }

    @Test
    void getHistory_returns_empty_for_room_with_no_messages() {
        given(roomParticipantPort.existsByRoomIdAndMemberId("room-99", "member-10")).willReturn(true);
        given(chatMessagePort.findTop50ByRoomId("room-99")).willReturn(List.of());

        List<ChatMessageResponse> history = chatService.getHistory("room-99", "member-10");

        assertThat(history).isEmpty();
    }
}
