import React, { useEffect, useState } from 'react';
import { Alert, Modal, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { showAlert } from '../utils/alert';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { RootStackParamList } from '../navigation/AppNavigator';

type RouteParams = RouteProp<RootStackParamList, 'RoomDetail'>;

const STATUS_LABEL: Record<string, string> = {
  OPEN: '모집 중', CLOSED: '마감', CONFIRMED: '확정', DELIVERING: '배달 중', COMPLETED: '완료', CANCELLED: '취소',
};

export function RoomDetailScreen() {
  const route = useRoute<RouteParams>();
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { roomId } = route.params;
  const { memberId } = useAuth();
  const qc = useQueryClient();
  const [menuName, setMenuName] = useState('');
  const [menuPrice, setMenuPrice] = useState('');
  const [aiModalVisible, setAiModalVisible] = useState(false);
  const [aiResult, setAiResult] = useState<{ restaurantName: string; reason: string } | null>(null);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['room', roomId],
    queryFn: () => apiClient.get(`/api/rooms/${roomId}`),
  });

  const { data: ordersData, refetch: refetchOrders } = useQuery({
    queryKey: ['orders', roomId],
    queryFn: () => apiClient.get(`/api/rooms/${roomId}/orders`),
  });

  const { data: participantsData } = useQuery({
    queryKey: ['participants', roomId],
    queryFn: () => apiClient.get(`/api/rooms/${roomId}/participants`),
  });

  const room: any = (data as any)?.data;
  const orders: any[] = (ordersData as any)?.data ?? [];
  const participants: any[] = (participantsData as any)?.data ?? [];
  const isHost: boolean = room?.isHost ?? false;
  const isParticipant: boolean = room?.isParticipant ?? false;
  const nicknameMap: Record<string, string> = Object.fromEntries(
    participants.map((p: any) => [p.memberId, p.nickname])
  );

  useEffect(() => {
    if (!room) return;
    if (room.status !== 'OPEN' && !isParticipant && !isHost) {
      showAlert('알림', '모집이 마감된 방입니다.');
      navigation.goBack();
    }
  }, [room?.status, isParticipant, isHost]);

  useEffect(() => {
    const unsubscribe = navigation.addListener('beforeRemove', (e) => {
      if (!room || room.status !== 'OPEN') return;
      e.preventDefault();
      if (isHost) {
        Alert.alert(
          '방 나가기',
          '방을 나가면 방이 취소됩니다. 나가시겠습니까?',
          [
            { text: '취소', style: 'cancel' },
            {
              text: '나가기',
              style: 'destructive',
              onPress: async () => {
                try {
                  await apiClient.post(`/api/rooms/${roomId}/cancel`);
                  qc.invalidateQueries({ queryKey: ['rooms'] });
                  navigation.dispatch(e.data.action);
                } catch (err: any) {
                  showAlert('오류', (err as any)?.message ?? '방 취소에 실패했습니다.');
                }
              },
            },
          ]
        );
      } else if (isParticipant) {
        Alert.alert(
          '방 나가기',
          '방을 나가면 추가한 메뉴가 삭제됩니다. 나가시겠습니까?',
          [
            { text: '취소', style: 'cancel' },
            {
              text: '나가기',
              style: 'destructive',
              onPress: async () => {
                try {
                  await apiClient.post(`/api/rooms/${roomId}/leave`);
                  qc.invalidateQueries({ queryKey: ['room', roomId] });
                  qc.invalidateQueries({ queryKey: ['participants', roomId] });
                  qc.invalidateQueries({ queryKey: ['orders', roomId] });
                  navigation.dispatch(e.data.action);
                } catch (err: any) {
                  showAlert('오류', (err as any)?.message ?? '방 나가기에 실패했습니다.');
                }
              },
            },
          ]
        );
      } else {
        navigation.dispatch(e.data.action);
      }
    });
    return unsubscribe;
  }, [navigation, isHost, isParticipant, roomId, room?.status]);

  function mutate(fn: () => Promise<any>, extraInvalidations?: string[][]) {
    return fn().then(() => {
      qc.invalidateQueries({ queryKey: ['room', roomId] });
      qc.invalidateQueries({ queryKey: ['participants', roomId] });
      qc.invalidateQueries({ queryKey: ['rooms'] });
      extraInvalidations?.forEach((key) => qc.invalidateQueries({ queryKey: key }));
      refetch();
    }).catch((e: any) => showAlert('오류', e?.message ?? '요청 실패'));
  }

  const addMenuMutation = useMutation({
    mutationFn: () => apiClient.post(`/api/rooms/${roomId}/orders`, { menuName, quantity: 1, price: parseInt(menuPrice, 10) }),
    onSuccess: () => { setMenuName(''); setMenuPrice(''); refetch(); refetchOrders(); },
    onError: (e: any) => showAlert('오류', e?.message),
  });

  const deleteOrderMutation = useMutation({
    mutationFn: (itemId: number) => apiClient.delete(`/api/rooms/${roomId}/orders/${itemId}`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['orders', roomId] }); refetchOrders(); },
    onError: (e: any) => showAlert('오류', e?.message ?? '삭제 실패'),
  });

  const aiRecommendMutation = useMutation({
    mutationFn: () => apiClient.post(`/api/rooms/${roomId}/recommend`),
    onSuccess: (res: any) => {
      const top = res?.recommendations?.[0];
      if (top) {
        setAiResult({ restaurantName: top.restaurantName, reason: top.reason });
        setAiModalVisible(true);
      } else {
        showAlert('AI 추천', '추천 결과가 없습니다.');
      }
    },
    onError: (e: any) => showAlert('오류', e?.message ?? 'AI 추천 실패'),
  });

  if (isLoading || !room) return <View style={styles.center}><Text>로딩 중...</Text></View>;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <Text style={styles.title}>{room.title}</Text>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
          {isHost && <View style={styles.hostBadge}><Text style={styles.hostBadgeText}>방장</Text></View>}
          <Text style={styles.status}>{STATUS_LABEL[room.status] ?? room.status}</Text>
        </View>
      </View>
      <Text style={styles.meta}>{room.restaurantName} · {room.restaurantAddress}</Text>
      <Text style={styles.meta}>배달비 {room.deliveryFee.toLocaleString()}원 · {room.currentParticipantCount}/{room.maxParticipants}명</Text>

      <View style={styles.menuSection}>
        <Text style={styles.sectionTitle}>참여자 ({participants.length}명)</Text>
        {participants.map((p: any) => (
          <View key={p.memberId} style={styles.participantItem}>
            <Text style={styles.participantName}>{p.nickname}</Text>
            {p.isHost && <View style={styles.hostBadge}><Text style={styles.hostBadgeText}>방장</Text></View>}
          </View>
        ))}
      </View>

      <View style={styles.menuSection}>
        <Text style={styles.sectionTitle}>주문 목록</Text>
        {orders.length === 0
          ? <Text style={styles.emptyText}>아직 추가된 메뉴가 없습니다.</Text>
          : orders.map((o: any) => (
              <View key={o.id} style={styles.orderItem}>
                <View>
                  <Text style={styles.orderName}>{o.menuName} x{o.quantity}</Text>
                  <Text style={styles.orderOwner}>{nicknameMap[o.memberId] ?? '알 수 없음'}</Text>
                </View>
                <Text style={styles.orderPrice}>{o.price.toLocaleString()}원</Text>
                {room.status === 'OPEN' && o.memberId === memberId && (
                  <TouchableOpacity
                    style={styles.deleteButton}
                    onPress={() => deleteOrderMutation.mutate(o.id)}
                    disabled={deleteOrderMutation.isPending}
                  >
                    <Text style={styles.deleteButtonText}>삭제</Text>
                  </TouchableOpacity>
                )}
              </View>
            ))
        }
      </View>

      {room.status === 'OPEN' && isParticipant && (
        <View style={styles.menuSection}>
          <Text style={styles.sectionTitle}>메뉴 추가</Text>
          <TextInput style={styles.input} value={menuName} onChangeText={setMenuName} placeholder="메뉴 이름" placeholderTextColor="#999" />
          <TextInput style={styles.input} value={menuPrice} onChangeText={setMenuPrice} placeholder="가격" keyboardType="numeric" placeholderTextColor="#999" />
          <TouchableOpacity style={styles.smallButton} onPress={() => addMenuMutation.mutate()}>
            <Text style={styles.smallButtonText}>추가</Text>
          </TouchableOpacity>
        </View>
      )}

      <View style={styles.actions}>
        {isHost && (room.status === 'OPEN' || room.status === 'CLOSED') && (
          <TouchableOpacity
            style={[styles.button, styles.dangerButton]}
            onPress={() => {
              Alert.alert(
                '방 취소',
                '방을 취소하면 모든 참여자에게 알림이 전송됩니다. 정말 취소하시겠습니까?',
                [
                  { text: '아니요', style: 'cancel' },
                  { text: '취소하기', style: 'destructive', onPress: async () => {
                      try {
                        await apiClient.post(`/api/rooms/${roomId}/cancel`);
                        qc.invalidateQueries({ queryKey: ['rooms'] });
                        navigation.goBack();
                      } catch (e: any) {
                        showAlert('오류', e?.message ?? '방 취소 실패');
                      }
                    }},
                ]
              );
            }}
          >
            <Text style={styles.buttonText}>방 취소 (나가기)</Text>
          </TouchableOpacity>
        )}
        {room.status === 'OPEN' && !isHost && !isParticipant && (
          <TouchableOpacity style={styles.button} onPress={() => mutate(() => apiClient.post(`/api/rooms/${roomId}/join`))}>
            <Text style={styles.buttonText}>참여하기</Text>
          </TouchableOpacity>
        )}
        {room.status === 'OPEN' && !isHost && isParticipant && (
          <TouchableOpacity style={styles.leaveButton} onPress={() => mutate(() => apiClient.post(`/api/rooms/${roomId}/leave`))}>
            <Text style={styles.leaveButtonText}>나가기</Text>
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
          <TouchableOpacity style={styles.button} onPress={() => {
              if (participants.length < 2) {
                showAlert('알림', '참여자가 2명 이상이어야 주문 확정이 가능합니다.');
                return;
              }
              const orderedMembers = new Set(orders.map((o: any) => o.memberId));
              const missing = participants.filter((p: any) => !orderedMembers.has(p.memberId));
              if (missing.length > 0) {
                showAlert('알림', `${missing.map((p: any) => p.nickname).join(', ')}님이 아직 주문을 추가하지 않았습니다.`);
                return;
              }
              mutate(() => apiClient.post(`/api/rooms/${roomId}/orders/confirm`));
            }}>
            <Text style={styles.buttonText}>주문 확정</Text>
          </TouchableOpacity>
        )}
        {isHost && room.status === 'CONFIRMED' && (
          <TouchableOpacity style={styles.button} onPress={() => mutate(() => apiClient.post(`/api/rooms/${roomId}/deliver`))}>
            <Text style={styles.buttonText}>배달 시작</Text>
          </TouchableOpacity>
        )}
        {isHost && room.status === 'DELIVERING' && (
          <TouchableOpacity style={styles.button} onPress={() => mutate(() => apiClient.post(`/api/rooms/${roomId}/complete`))}>
            <Text style={styles.buttonText}>완료 처리</Text>
          </TouchableOpacity>
        )}
        {(room.status === 'CONFIRMED' || room.status === 'DELIVERING' || room.status === 'COMPLETED') && (
          <TouchableOpacity style={styles.button} onPress={() => navigation.navigate('Settlement', { roomId })}>
            <Text style={styles.buttonText}>정산 내역</Text>
          </TouchableOpacity>
        )}
        {isParticipant && (
          <TouchableOpacity style={styles.button} onPress={() => navigation.navigate('Chat', { roomId, roomTitle: room.title })}>
            <Text style={styles.buttonText}>채팅</Text>
          </TouchableOpacity>
        )}
        {isHost && room.status === 'OPEN' && (
          <TouchableOpacity
            style={[styles.button, styles.aiButton]}
            onPress={() => aiRecommendMutation.mutate()}
            disabled={aiRecommendMutation.isPending}
          >
            <Text style={styles.buttonText}>{aiRecommendMutation.isPending ? 'AI 추천 중...' : 'AI 맛집 추천'}</Text>
          </TouchableOpacity>
        )}
      </View>

      <Modal visible={aiModalVisible} transparent animationType="fade" onRequestClose={() => setAiModalVisible(false)}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalBox}>
            <Text style={styles.modalTitle}>AI 추천 결과</Text>
            {aiResult && (
              <>
                <Text style={styles.modalRestaurant}>{aiResult.restaurantName}</Text>
                <Text style={styles.modalReason}>{aiResult.reason}</Text>
              </>
            )}
            <TouchableOpacity style={styles.smallButton} onPress={() => setAiModalVisible(false)}>
              <Text style={styles.smallButtonText}>닫기</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
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
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 10, fontSize: 14, marginBottom: 8, color: '#333', backgroundColor: '#fff' },
  smallButton: { backgroundColor: '#FF6B35', borderRadius: 6, padding: 10, alignItems: 'center' },
  smallButtonText: { color: '#fff', fontWeight: '600' },
  emptyText: { color: '#aaa', fontSize: 13 },
  orderItem: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 6, borderBottomWidth: 1, borderColor: '#eee' },
  orderName: { fontSize: 14, color: '#333' },
  orderOwner: { fontSize: 11, color: '#999' },
  orderPrice: { fontSize: 14, color: '#FF6B35', fontWeight: '600' },
  actions: { marginTop: 24, gap: 10 },
  button: { backgroundColor: '#FF6B35', borderRadius: 8, padding: 14, alignItems: 'center' },
  buttonText: { color: '#fff', fontWeight: 'bold', fontSize: 15 },
  leaveButton: { borderWidth: 1, borderColor: '#aaa', borderRadius: 8, padding: 12, alignItems: 'center', marginTop: 4 },
  leaveButtonText: { color: '#888', fontWeight: '600', fontSize: 14 },
  dangerButton: { backgroundColor: '#e74c3c' },
  aiButton: { backgroundColor: '#6c5ce7' },
  deleteButton: { paddingHorizontal: 8, paddingVertical: 4, backgroundColor: '#e74c3c', borderRadius: 4 },
  deleteButtonText: { color: '#fff', fontSize: 12, fontWeight: '600' },
  participantItem: { flexDirection: 'row', alignItems: 'center', paddingVertical: 6, gap: 8 },
  participantName: { fontSize: 14, color: '#333', flex: 1 },
  hostBadge: { backgroundColor: '#FF6B35', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 4 },
  hostBadgeText: { color: '#fff', fontSize: 11, fontWeight: '700' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
  modalBox: { backgroundColor: '#fff', borderRadius: 12, padding: 24, width: '80%', gap: 12 },
  modalTitle: { fontSize: 18, fontWeight: 'bold', textAlign: 'center' },
  modalRestaurant: { fontSize: 16, fontWeight: '600', color: '#FF6B35', textAlign: 'center' },
  modalReason: { fontSize: 14, color: '#555', textAlign: 'center', lineHeight: 20 },
});
