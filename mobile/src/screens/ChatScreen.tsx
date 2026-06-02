import React, { useCallback, useEffect, useRef, useState } from 'react';
import { FlatList, KeyboardAvoidingView, Platform, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRoute, RouteProp } from '@react-navigation/native';
import { useQuery } from '@tanstack/react-query';
import { Client } from '@stomp/stompjs';
import { apiClient } from '../api/client';
import { showAlert } from '../utils/alert';
import { getItem } from '../utils/storage';
import { ChatBubble } from '../components/ChatBubble';
import { useAuth } from '../context/AuthContext';
import { RootStackParamList } from '../navigation/AppNavigator';

type RouteParams = RouteProp<RootStackParamList, 'Chat'>;

interface ChatMessage {
  id: string;
  roomId: string;
  memberId?: string;
  nickname?: string;
  type: 'TALK' | 'ENTER' | 'LEAVE' | 'NOTICE';
  content: string;
  createdAt: string;
}

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
const WS_URL = process.env.EXPO_PUBLIC_WS_URL ?? API_BASE_URL.replace(/^http/, 'ws');

export function ChatScreen() {
  const { roomId } = useRoute<RouteParams>().params;
  const { memberId } = useAuth();
  const insets = useSafeAreaInsets();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState('');
  const [connected, setConnected] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);
  const stompClient = useRef<Client | null>(null);
  const listRef = useRef<FlatList>(null);

  const { data: historyData } = useQuery({
    queryKey: ['chatHistory', roomId],
    queryFn: () => apiClient.get(`/api/rooms/${roomId}/chats`),
  });

  useEffect(() => {
    const history: ChatMessage[] = (historyData as any)?.data ?? [];
    setMessages([...history].reverse());
  }, [historyData]);

  useEffect(() => {
    let mounted = true;

    async function connect() {
      const token = await getItem('deviceToken');
      const brokerURL = `${WS_URL}/ws-native`;
      console.warn('[ChatScreen] STOMP connecting', {
        brokerURL,
        roomId,
        hasToken: Boolean(token),
        tokenLength: token?.length ?? 0,
      });
      const client = new Client({
        brokerURL,
        webSocketFactory: () => new WebSocket(brokerURL),
        connectHeaders: { 'X-Device-Token': token ?? '' },
        reconnectDelay: 2000,
        connectionTimeout: 5000,
        heartbeatIncoming: 0,
        heartbeatOutgoing: 0,
        debug: (message) => console.warn('[ChatScreen] STOMP debug', message),
        onConnect: () => {
          if (!mounted) return;
          console.warn('[ChatScreen] STOMP connected', { roomId });
          setConnected(true);
          setConnectionError(null);
          client.subscribe(`/topic/room/${roomId}`, (frame) => {
            if (!mounted) return;
            console.warn('[ChatScreen] STOMP message', { roomId, bytes: frame.body?.length ?? 0 });
            const msg: ChatMessage = JSON.parse(frame.body);
            setMessages((prev) => [...prev, msg]);
          });
        },
        onDisconnect: () => {
          if (!mounted) return;
          console.warn('[ChatScreen] STOMP disconnected', { roomId });
          setConnected(false);
        },
        onStompError: (frame) => {
          if (!mounted) return;
          const message = frame.headers?.message ?? '연결에 실패했습니다.';
          console.warn('[ChatScreen] STOMP error', { headers: frame.headers, body: frame.body });
          setConnected(false);
          setConnectionError(message);
          showAlert('채팅 오류', message);
        },
        onWebSocketError: (event) => {
          if (!mounted) return;
          const message = '서버에 연결할 수 없습니다.';
          console.warn('[ChatScreen] WebSocket error', event);
          setConnected(false);
          setConnectionError(message);
          showAlert('채팅 오류', message);
        },
        onWebSocketClose: (event) => {
          if (!mounted) return;
          console.warn('[ChatScreen] WebSocket close', {
            code: event?.code,
            reason: event?.reason,
            wasClean: event?.wasClean,
          });
          setConnected(false);
          setConnectionError('채팅 서버 응답이 없습니다. 잠시 후 다시 시도해주세요.');
        },
      });
      client.activate();
      stompClient.current = client;
    }

    connect();
    return () => {
      mounted = false;
      stompClient.current?.deactivate();
    };
  }, [roomId]);

  const sendMessage = useCallback(() => {
    const text = inputText.trim();
    if (!text) return;
    if (!connected || !stompClient.current?.connected) {
      showAlert('연결 안됨', '채팅 서버에 연결되지 않았습니다. 방에 참여했는지 확인해주세요.');
      return;
    }
    stompClient.current.publish({
      destination: `/app/room/${roomId}/chat`,
      body: JSON.stringify({ type: 'TALK', content: text }),
    });
    setInputText('');
  }, [inputText, roomId, connected]);

  useEffect(() => {
    if (messages.length > 0) listRef.current?.scrollToEnd({ animated: true });
  }, [messages]);

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <FlatList
        ref={listRef}
        data={messages}
        keyExtractor={(item, index) => String(item.id ?? index)}
        renderItem={({ item }) => (
          <ChatBubble message={item} isMine={item.memberId === memberId} />
        )}
        contentContainerStyle={styles.messageList}
      />
      {!connected && (
        <View style={styles.disconnectedBanner}>
          <Text style={styles.disconnectedText}>{connectionError ?? '채팅 서버에 연결 중...'}</Text>
        </View>
      )}
      <View style={[styles.inputRow, { paddingBottom: insets.bottom + 4 }]}>
        <TextInput
          style={styles.input}
          value={inputText}
          onChangeText={setInputText}
          placeholder="메시지 입력..."
          onSubmitEditing={sendMessage}
          returnKeyType="send"
        />
        <TouchableOpacity style={styles.sendButton} onPress={sendMessage}>
          <Text style={styles.sendText}>전송</Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  messageList: { padding: 12, paddingBottom: 8 },
  inputRow: { flexDirection: 'row', padding: 8, backgroundColor: '#fff', borderTopWidth: 1, borderColor: '#eee' },
  input: { flex: 1, borderWidth: 1, borderColor: '#ddd', borderRadius: 20, paddingHorizontal: 14, paddingVertical: 8, fontSize: 15, marginRight: 8 },
  sendButton: { backgroundColor: '#FF6B35', borderRadius: 20, paddingHorizontal: 16, justifyContent: 'center' },
  sendText: { color: '#fff', fontWeight: 'bold' },
  disconnectedBanner: { backgroundColor: '#f0ad4e', padding: 6, alignItems: 'center' },
  disconnectedText: { color: '#fff', fontSize: 12 },
});
