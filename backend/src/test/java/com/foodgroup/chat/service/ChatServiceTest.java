package com.foodgroup.chat.service;

import com.foodgroup.auth.domain.Member;
import com.foodgroup.auth.repository.MemberRepository;
import com.foodgroup.chat.domain.ChatMessage;
import com.foodgroup.chat.domain.ChatMessageType;
import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.repository.ChatMessageRepository;
import com.foodgroup.common.exception.BusinessException;
import com.foodgroup.room.repository.RoomParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks ChatService chatService;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock MemberRepository memberRepository;
    @Mock RoomParticipantRepository roomParticipantRepository;
    @Mock SimpMessagingTemplate messagingTemplate;

    @Test
    void saveAndBroadcast_TALK_saves_and_broadcasts() {
        ChatMessage saved = ChatMessage.builder()
                .roomId(1L).memberId(10L).type(ChatMessageType.TALK).content("hello").build();
        Member member = Member.builder().id(10L).nickname("짱구").deviceToken("token").build();

        given(chatMessageRepository.save(any())).willReturn(saved);
        given(memberRepository.findById(10L)).willReturn(Optional.of(member));

        ChatMessageResponse response = chatService.saveAndBroadcast(1L, 10L, ChatMessageType.TALK, "hello");

        assertThat(response.nickname()).isEqualTo("짱구");
        assertThat(response.type()).isEqualTo(ChatMessageType.TALK);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/1"), any(ChatMessageResponse.class));
    }

    @Test
    void saveAndBroadcast_NOTICE_null_memberId_sets_no_nickname() {
        ChatMessage saved = ChatMessage.builder()
                .roomId(1L).memberId(null).type(ChatMessageType.NOTICE).content("방이 마감됩니다").build();

        given(chatMessageRepository.save(any())).willReturn(saved);

        ChatMessageResponse response = chatService.saveAndBroadcast(1L, null, ChatMessageType.NOTICE, "방이 마감됩니다");

        assertThat(response.nickname()).isNull();
        verify(memberRepository, never()).findById(any());
    }

    @Test
    void getHistory_throws_UNAUTHORIZED_when_not_participant() {
        given(roomParticipantRepository.existsByRoomIdAndMemberId(1L, 99L)).willReturn(false);

        assertThatThrownBy(() -> chatService.getHistory(1L, 99L))
                .isInstanceOf(BusinessException.class);
        verify(chatMessageRepository, never()).findTop50ByRoomIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getHistory_returns_messages_for_participant() {
        ChatMessage m1 = ChatMessage.builder().roomId(1L).memberId(10L).type(ChatMessageType.TALK).content("a").build();
        ChatMessage m2 = ChatMessage.builder().roomId(1L).memberId(10L).type(ChatMessageType.TALK).content("b").build();
        Member member = Member.builder().id(10L).nickname("짱구").deviceToken("token").build();

        given(roomParticipantRepository.existsByRoomIdAndMemberId(1L, 10L)).willReturn(true);
        given(chatMessageRepository.findTop50ByRoomIdOrderByCreatedAtDesc(1L)).willReturn(List.of(m1, m2));
        given(memberRepository.findById(10L)).willReturn(Optional.of(member));

        List<ChatMessageResponse> history = chatService.getHistory(1L, 10L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).nickname()).isEqualTo("짱구");
    }

    @Test
    void deleteOldMessages_calls_repository() {
        chatService.deleteOldMessages();
        verify(chatMessageRepository).deleteOldMessages(any(LocalDateTime.class));
    }

    @Test
    void saveAndBroadcast_broadcasts_to_correct_topic() {
        ChatMessage saved = ChatMessage.builder()
                .roomId(42L).memberId(5L).type(ChatMessageType.ENTER).content("입장").build();
        Member member = Member.builder().id(5L).nickname("철수").deviceToken("t").build();

        given(chatMessageRepository.save(any())).willReturn(saved);
        given(memberRepository.findById(5L)).willReturn(Optional.of(member));

        chatService.saveAndBroadcast(42L, 5L, ChatMessageType.ENTER, "입장");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), any(ChatMessageResponse.class));
        assertThat(topicCaptor.getValue()).isEqualTo("/topic/room/42");
    }

    @Test
    void getHistory_returns_empty_for_room_with_no_messages() {
        given(roomParticipantRepository.existsByRoomIdAndMemberId(99L, 10L)).willReturn(true);
        given(chatMessageRepository.findTop50ByRoomIdOrderByCreatedAtDesc(99L)).willReturn(List.of());

        List<ChatMessageResponse> history = chatService.getHistory(99L, 10L);

        assertThat(history).isEmpty();
    }
}
