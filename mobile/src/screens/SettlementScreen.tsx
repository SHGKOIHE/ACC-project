import React, { useEffect } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useRoute, RouteProp } from '@react-navigation/native';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../api/client';
import { showAlert } from '../utils/alert';
import { SettlementSummary } from '../components/SettlementSummary';
import { OrderItemRow } from '../components/OrderItemRow';
import { useAuth } from '../context/AuthContext';
import { RootStackParamList } from '../navigation/AppNavigator';

type RouteParams = RouteProp<RootStackParamList, 'Settlement'>;

export function SettlementScreen() {
  const { roomId } = useRoute<RouteParams>().params;
  const { memberId } = useAuth();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['settlement', roomId],
    queryFn: () => apiClient.get(`/api/rooms/${roomId}/orders/settlement`),
  });

  useEffect(() => {
    if (isError) showAlert('오류', '정산 정보를 불러오지 못했습니다.');
  }, [isError]);

  const { data: ordersData } = useQuery({
    queryKey: ['myOrders', roomId],
    queryFn: () => apiClient.get(`/api/rooms/${roomId}/orders`, { params: { mine: true } }),
  });

  const settlement: any = (data as any)?.data;
  const myOrders: any[] = (ordersData as any)?.data ?? [];

  if (isLoading) return <View style={styles.center}><Text>로딩 중...</Text></View>;
  if (!settlement) return <View style={styles.center}><Text>정산 정보가 없습니다</Text></View>;

  const myMember = settlement.members?.find((m: any) => m.memberId === memberId);
  const totalAmount: number = myMember?.totalAmount ?? 0;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <SettlementSummary
        totalMenuAmount={myMember?.menuAmount ?? 0}
        deliveryFeePerPerson={settlement.deliveryFeePerPerson}
        totalAmount={totalAmount}
        bankName={settlement.bankName ?? '-'}
        accountHolder={settlement.accountHolder ?? '-'}
        accountNumber={settlement?.accountNumber ?? ''}
      />
      <Text style={styles.sectionTitle}>내 주문 내역</Text>
      {myOrders.length === 0
        ? <Text style={styles.emptyText}>주문 내역이 없습니다.</Text>
        : myOrders.map((item: any) => (
            <OrderItemRow key={item.id} menuName={item.menuName} price={item.price} />
          ))
      }
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { padding: 20 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  sectionTitle: { fontSize: 16, fontWeight: '600', marginTop: 24, marginBottom: 12 },
  emptyText: { color: '#aaa', fontSize: 14 },
});
