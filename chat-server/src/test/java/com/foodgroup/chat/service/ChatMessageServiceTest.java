package com.foodgroup.chat.service;

import com.foodgroup.chat.dto.ChatMessageResponse;
import com.foodgroup.chat.dto.ChatMessageType;
import com.foodgroup.chat.pubsub.RedisChatPublisher;
import com.foodgroup.chat.repository.ChatMessageWriter;
import com.foodgroup.chat.repository.RoomParticipantChecker;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatMessageServiceTest {

    @Test
    void savesThenPublishesWhenSenderIsRoomParticipant() {
        RoomParticipantChecker checker = mock(RoomParticipantChecker.class);
        when(checker.isParticipant("room-1", "member-1")).thenReturn(true);
        ChatMessageWriter writer = mock(ChatMessageWriter.class);
        RedisChatPublisher publisher = mock(RedisChatPublisher.class);
        ChatMessageService service = new ChatMessageService(checker, writer, publisher);

        ChatMessageResponse message = service.saveAndPublish("room-1", "member-1", ChatMessageType.TALK, "hello");

        assertThat(message.roomId()).isEqualTo("room-1");
        assertThat(message.memberId()).isEqualTo("member-1");
        assertThat(message.content()).isEqualTo("hello");

        InOrder inOrder = inOrder(writer, publisher);
        inOrder.verify(writer).save(message);
        inOrder.verify(publisher).publish(message);
    }

    @Test
    void rejectsAndNeverPersistsOrPublishesWhenSenderIsNotRoomParticipant() {
        RoomParticipantChecker checker = mock(RoomParticipantChecker.class);
        when(checker.isParticipant("room-1", "member-1")).thenReturn(false);
        ChatMessageWriter writer = mock(ChatMessageWriter.class);
        RedisChatPublisher publisher = mock(RedisChatPublisher.class);
        ChatMessageService service = new ChatMessageService(checker, writer, publisher);

        assertThatThrownBy(() -> service.saveAndPublish("room-1", "member-1", ChatMessageType.TALK, "hello"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not a room participant");

        verifyNoInteractions(writer, publisher);
    }

    @Test
    void rejectsAnonymousSenderWithoutCheckingParticipation() {
        RoomParticipantChecker checker = mock(RoomParticipantChecker.class);
        ChatMessageWriter writer = mock(ChatMessageWriter.class);
        RedisChatPublisher publisher = mock(RedisChatPublisher.class);
        ChatMessageService service = new ChatMessageService(checker, writer, publisher);

        assertThatThrownBy(() -> service.saveAndPublish("room-1", null, ChatMessageType.TALK, "hello"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(writer, publisher);
    }
}
