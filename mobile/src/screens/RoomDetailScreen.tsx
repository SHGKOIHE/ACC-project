import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { showAlert } from '../utils/alert';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { RootStackParamList } from '../navigation/AppNavigator';

type RouteParams = RouteProp<RootStackParamList, 'RoomDetail'>;

const STATUS_LABEL: Record<string, string> = {
  OPEN: '모집 중', CLOSED: '마감', CONFIRMED: '확정', COMPLETED: '완료', CANCELLED: '취소',
};

export function RoomDetailScreen() {
  const route = useRoute<RouteParams>();
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { roomId } = route.params;
  const { memberId } = useAuth();
  const qc = useQueryClient();
  const [menuName, setMenuName] = useState('');
  const [menuPrice, setMenuPrice] = useState('');

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['room', roomId],
    queryFn: () => apiClient.get(`/api/rooms/${roomId}`),
  });

  const { data: ordersData, refetch: refetchOrders } = useQuery({
    queryKey: ['orders', roomId],
    queryFn: () => apiClient.get(`/api/rooms/${roomId}/orders`),
  });

  const room: any = (data as any)?.data;
  const orders: any[] = (ordersData as any)?.data ?? [];
  const isHost = room?.hostId === memberId;
  const isParticipant: boolean = room?.isParticipant ?? false;

  function mutate(fn: () => Promise<any>) {
    return fn().then(() => { qc.invalidateQueries({ queryKey: ['room', roomId] }); refetch(); })
               .catch((e: any) => showAlert('오류', e?.message ?? '요청 실패'));
  }

  const addMenuMutation = useMutation({
    mutationFn: () => apiClient.post(`/api/rooms/${roomId}/orders`, { menuName, quantity: 1, price: parseInt(menuPrice, 10) }),
    onSuccess: () => { setMenuName(''); setMenuPrice(''); refetch(); refetchOrders(); },
    onError: (e: any) => showAlert('오류', e?.message),
  });

  if (isLoading || !room) return <View style={styles.center}><Text>로딩 중...</Text></View>;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <Text style={styles.title}>{room.title}</Text>
        <Text style={styles.status}>{STATUS_LABEL[room.status] ?? room.status}</Text>
      </View>
      <Text style={styles.meta}>{room.restaurantName} · {room.restaurantAddress}</Text>
      <Text style={styles.meta}>배달비 {room.deliveryFee.toLocaleString()}원 · {room.currentParticipantCount}/{room.maxParticipants}명</Text>

      <View style={styles.menuSection}>
        <Text style={styles.sectionTitle}>주문 목록</Text>
        {orders.length === 0
          ? <Text style={styles.emptyText}>아직 추가된 메뉴가 없습니다.</Text>
          : orders.map((o: any) => (
              <View key={o.id} style={styles.orderItem}>
                <Text style={styles.orderName}>{o.menuName} x{o.quantity}</Text>
                <Text style={styles.orderPrice}>{o.price.toLocaleString()}원</Text>
              </View>
            ))
        }
      </View>

      {room.status === 'OPEN' && isParticipant && (
        <View style={styles.menuSection}>
          <Text style={styles.sectionTitle}>메뉴 추가</Text>
          <TextInput style={styles.input} value={menuName} onChangeText={setMenuName} placeholder="메뉴 이름" />
          <TextInput style={styles.input} value={menuPrice} onChangeText={setMenuPrice} placeholder="가격" keyboardType="numeric" />
          <TouchableOpacity style={styles.smallButton} onPress={() => addMenuMutation.mutate()}>
            <Text style={styles.smallButtonText}>추가</Text>
          </TouchableOpacity>
        </View>
      )}

      <View style={styles.actions}>
        {room.status === 'OPEN' && !isHost && !isParticipant && (
          <TouchableOpacity style={styles.button} onPress={() => mutate(() => apiClient.post(`/api/rooms/${roomId}/join`))}>
            <Text style={styles.buttonText}>참여하기</Text>
          </TouchableOpacity>
        )}
        {room.status === 'OPEN' && !isHost && isParticipant && (
          <TouchableOpacity style={[styles.button, styles.outlineButton]} onPress={() => mutate(() => apiClient.post(`/api/rooms/${roomId}/leave`))}>
            <Text style={styles.outlineButtonText}>탈퇴하기</Text>
          </TouchableOpacity>
        )}
        {isHost && room.status === 'OPEN' && (
          <TouchableOpacity
            style={styles.button}
            onPress={() => {
              if (orders.length === 0) {
                showAlert('알림', '주문을 먼저 추가해주세요.');
                return;
              }
              mutate(() => apiClient.post(`/api/rooms/${roomId}/close`));
            }}
          >
            <Text style={styles.buttonText}>마감하기</Text>
          </TouchableOpacity>
        )}
        {isHost && room.status === 'CLOSED' && (
          <TouchableOpacity style={styles.button} onPress={() => mutate(() => apiClient.post(`/api/rooms/${roomId}/orders/confirm`))}>
            <Text style={styles.buttonText}>주문 확정</Text>
          </TouchableOpacity>
        )}
        {isHost && room.status === 'CONFIRMED' && (
          <TouchableOpacity style={styles.button} onPress={() => mutate(() => apiClient.post(`/api/rooms/${roomId}/complete`))}>
            <Text style={styles.buttonText}>완료 처리</Text>
          </TouchableOpacity>
        )}
        {(room.status === 'CONFIRMED' || room.status === 'COMPLETED') && (
          <TouchableOpacity style={styles.button} onPress={() => navigation.navigate('Settlement', { roomId })}>
            <Text style={styles.buttonText}>정산 내역</Text>
          </TouchableOpacity>
        )}
        {isParticipant && (
          <TouchableOpacity style={styles.button} onPress={() => navigation.navigate('Chat', { roomId, roomTitle: room.title })}>
            <Text style={styles.buttonText}>채팅</Text>
          </TouchableOpacity>
        )}
        {isHost && (room.status === 'OPEN' || room.status === 'CLOSED') && (
          <TouchableOpacity style={[styles.button, styles.dangerButton]} onPress={() => mutate(() => apiClient.post(`/api/rooms/${roomId}/cancel`))}>
            <Text style={styles.buttonText}>방 취소</Text>
          </TouchableOpacity>
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { padding: 20 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  title: { fontSize: 20, fontWeight: 'bold', flex: 1 },
  status: { fontSize: 13, color: '#FF6B35', fontWeight: '600' },
  meta: { fontSize: 14, color: '#666', marginBottom: 4 },
  sectionTitle: { fontSize: 16, fontWeight: '600', marginBottom: 8 },
  menuSection: { marginTop: 20, padding: 16, backgroundColor: '#f9f9f9', borderRadius: 8 },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 10, fontSize: 14, marginBottom: 8 },
  smallButton: { backgroundColor: '#FF6B35', borderRadius: 6, padding: 10, alignItems: 'center' },
  smallButtonText: { color: '#fff', fontWeight: '600' },
  emptyText: { color: '#aaa', fontSize: 13 },
  orderItem: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 6, borderBottomWidth: 1, borderColor: '#eee' },
  orderName: { fontSize: 14, color: '#333' },
  orderPrice: { fontSize: 14, color: '#FF6B35', fontWeight: '600' },
  actions: { marginTop: 24, gap: 10 },
  button: { backgroundColor: '#FF6B35', borderRadius: 8, padding: 14, alignItems: 'center' },
  buttonText: { color: '#fff', fontWeight: 'bold', fontSize: 15 },
  outlineButton: { backgroundColor: '#fff', borderWidth: 1, borderColor: '#FF6B35' },
  outlineButtonText: { color: '#FF6B35', fontWeight: 'bold', fontSize: 15 },
  dangerButton: { backgroundColor: '#e74c3c' },
});
