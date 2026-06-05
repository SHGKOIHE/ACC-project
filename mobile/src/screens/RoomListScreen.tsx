import React, { useCallback, useEffect, useState } from 'react';
import { FlatList, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { showAlert } from '../utils/alert';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../api/client';
import { RoomCard } from '../components/RoomCard';
import { RootStackParamList } from '../navigation/AppNavigator';

type MeetingType = 'DELIVERY' | 'DELIVERY_TOGETHER' | 'DINE_OUT';

const ROOM_LIST_REFETCH_INTERVAL_MS = 5000;

const FILTERS: { label: string; value: MeetingType | 'ALL' }[] = [
  { label: '전체', value: 'ALL' },
  { label: '배달', value: 'DELIVERY' },
  { label: '같이배달', value: 'DELIVERY_TOGETHER' },
  { label: '같이먹기', value: 'DINE_OUT' },
];

export function RoomListScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const [filter, setFilter] = useState<MeetingType | 'ALL'>('ALL');
  const insets = useSafeAreaInsets();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['rooms', filter],
    queryFn: () => apiClient.get('/api/rooms', { params: filter !== 'ALL' ? { meetingType: filter } : {} }),
    refetchInterval: ROOM_LIST_REFETCH_INTERVAL_MS,
    refetchIntervalInBackground: false,
    refetchOnMount: 'always',
  });

  const rooms: any[] = (data as any)?.data ?? [];

  useFocusEffect(
    useCallback(() => {
      refetch();
    }, [refetch])
  );

  useEffect(() => {
    if (isError) showAlert('오류', '방 목록을 불러오지 못했습니다.');
  }, [isError]);

  return (
    <View style={styles.container}>
      <View style={[styles.filterRow, { paddingTop: insets.top + 8 }]}>
        {FILTERS.map((f) => (
          <TouchableOpacity
            key={f.value}
            style={[styles.filterChip, filter === f.value && styles.filterChipActive]}
            onPress={() => setFilter(f.value)}
          >
            <Text style={[styles.filterText, filter === f.value && styles.filterTextActive]}>{f.label}</Text>
          </TouchableOpacity>
        ))}
        <TouchableOpacity onPress={() => refetch()} style={styles.refreshButton}>
          <Text style={styles.refreshText}>↻</Text>
        </TouchableOpacity>
      </View>
      <FlatList
        data={rooms}
        keyExtractor={(item) => String(item.id)}
        renderItem={({ item }) => (
          <RoomCard room={item} onPress={() => navigation.navigate('RoomDetail', { roomId: item.id })} />
        )}
        onRefresh={refetch}
        refreshing={isLoading}
        contentContainerStyle={rooms.length === 0 ? styles.empty : undefined}
        ListEmptyComponent={!isLoading ? <Text style={styles.emptyText}>진행 중인 방이 없습니다</Text> : null}
      />
      <TouchableOpacity style={styles.fab} onPress={() => navigation.navigate('CreateRoom')}>
        <Text style={styles.fabText}>+ 방 만들기</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  filterRow: { flexDirection: 'row', padding: 12, gap: 8, backgroundColor: '#fff' },
  filterChip: { paddingHorizontal: 14, paddingVertical: 6, borderRadius: 20, borderWidth: 1, borderColor: '#ddd' },
  filterChipActive: { backgroundColor: '#FF6B35', borderColor: '#FF6B35' },
  filterText: { fontSize: 13, color: '#666' },
  filterTextActive: { color: '#fff', fontWeight: '600' },
  empty: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  emptyText: { color: '#999', fontSize: 15 },
  fab: { position: 'absolute', bottom: 24, right: 24, backgroundColor: '#FF6B35', borderRadius: 24, paddingHorizontal: 20, paddingVertical: 14, elevation: 4 },
  fabText: { color: '#fff', fontWeight: 'bold', fontSize: 15 },
  refreshButton: { marginLeft: 'auto', paddingHorizontal: 10, paddingVertical: 6, justifyContent: 'center' },
  refreshText: { fontSize: 18, color: '#FF6B35' },
});
