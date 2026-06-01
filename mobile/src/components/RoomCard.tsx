import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';

const MEETING_TYPE_LABEL: Record<string, string> = {
  DELIVERY: '배달',
  DELIVERY_TOGETHER: '같이배달',
  DINE_OUT: '같이먹기',
};

const STATUS_COLOR: Record<string, string> = {
  OPEN: '#27ae60',
  CLOSED: '#e67e22',
  CONFIRMED: '#2980b9',
  COMPLETED: '#95a5a6',
  CANCELLED: '#e74c3c',
};

interface Props {
  room: {
    id: string;
    title: string;
    meetingType: string;
    restaurantName: string;
    currentParticipantCount: number;
    maxParticipants: number;
    deliveryFee: number;
    status: string;
  };
  onPress: () => void;
}

export function RoomCard({ room, onPress }: Props) {
  return (
    <TouchableOpacity style={styles.card} onPress={onPress} activeOpacity={0.8}>
      <View style={styles.row}>
        <Text style={styles.title} numberOfLines={1}>{room.title}</Text>
        <View style={[styles.badge, { backgroundColor: STATUS_COLOR[room.status] ?? '#999' }]}>
          <Text style={styles.badgeText}>{room.status}</Text>
        </View>
      </View>
      <Text style={styles.restaurant}>{room.restaurantName}</Text>
      <View style={styles.footer}>
        <Text style={styles.meta}>{MEETING_TYPE_LABEL[room.meetingType] ?? room.meetingType}</Text>
        <Text style={styles.meta}>{room.currentParticipantCount}/{room.maxParticipants}명</Text>
        <Text style={styles.meta}>배달비 {room.deliveryFee.toLocaleString()}원</Text>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: { backgroundColor: '#fff', borderRadius: 12, padding: 16, marginHorizontal: 12, marginVertical: 6, elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.08, shadowRadius: 4 },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 },
  title: { fontSize: 16, fontWeight: '600', flex: 1, marginRight: 8 },
  badge: { borderRadius: 10, paddingHorizontal: 8, paddingVertical: 2 },
  badgeText: { color: '#fff', fontSize: 11, fontWeight: '600' },
  restaurant: { fontSize: 13, color: '#666', marginBottom: 8 },
  footer: { flexDirection: 'row', gap: 12 },
  meta: { fontSize: 12, color: '#999' },
});
