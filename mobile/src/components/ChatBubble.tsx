import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

interface ChatMessage {
  id: number;
  memberId?: number;
  nickname?: string;
  type: 'TALK' | 'ENTER' | 'LEAVE' | 'NOTICE';
  content: string;
  createdAt: string;
}

interface Props {
  message: ChatMessage;
  isMine: boolean;
}

export function ChatBubble({ message, isMine }: Props) {
  if (message.type === 'NOTICE' || message.type === 'ENTER' || message.type === 'LEAVE') {
    return (
      <View style={styles.noticeRow}>
        <Text style={styles.noticeText}>{message.content}</Text>
      </View>
    );
  }

  const time = new Date(message.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });

  return (
    <View style={[styles.row, isMine && styles.rowMine]}>
      {!isMine && (
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>{message.nickname?.charAt(0) ?? '?'}</Text>
        </View>
      )}
      <View style={styles.bubbleGroup}>
        {!isMine && <Text style={styles.nickname}>{message.nickname}</Text>}
        <View style={[styles.bubble, isMine ? styles.bubbleMine : styles.bubbleOther]}>
          <Text style={[styles.content, isMine && styles.contentMine]}>{message.content}</Text>
        </View>
        <Text style={[styles.time, isMine && styles.timeMine]}>{time}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', marginVertical: 4, alignItems: 'flex-end' },
  rowMine: { justifyContent: 'flex-end' },
  noticeRow: { alignItems: 'center', marginVertical: 8 },
  noticeText: { fontSize: 12, color: '#999', backgroundColor: '#eee', paddingHorizontal: 12, paddingVertical: 4, borderRadius: 12 },
  avatar: { width: 32, height: 32, borderRadius: 16, backgroundColor: '#FF6B35', justifyContent: 'center', alignItems: 'center', marginRight: 8, marginBottom: 16 },
  avatarText: { color: '#fff', fontWeight: 'bold', fontSize: 13 },
  bubbleGroup: { maxWidth: '70%' },
  nickname: { fontSize: 11, color: '#999', marginBottom: 2, marginLeft: 4 },
  bubble: { borderRadius: 16, paddingHorizontal: 14, paddingVertical: 10 },
  bubbleOther: { backgroundColor: '#fff', borderWidth: 1, borderColor: '#eee' },
  bubbleMine: { backgroundColor: '#FF6B35' },
  content: { fontSize: 15, color: '#333', lineHeight: 20 },
  contentMine: { color: '#fff' },
  time: { fontSize: 11, color: '#bbb', marginTop: 2, marginLeft: 4 },
  timeMine: { textAlign: 'right', marginRight: 4 },
});
