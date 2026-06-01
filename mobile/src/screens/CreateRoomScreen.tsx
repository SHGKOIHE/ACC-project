import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import * as Location from 'expo-location';
import { showAlert } from '../utils/alert';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../api/client';
import { RootStackParamList } from '../navigation/AppNavigator';

type MeetingType = 'DELIVERY' | 'DELIVERY_TOGETHER' | 'DINE_OUT';

const MEETING_TYPES: { label: string; value: MeetingType }[] = [
  { label: '배달 (각자)', value: 'DELIVERY' },
  { label: '같이 배달', value: 'DELIVERY_TOGETHER' },
  { label: '같이 먹기', value: 'DINE_OUT' },
];

export function CreateRoomScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const qc = useQueryClient();

  const [form, setForm] = useState({
    title: '',
    meetingType: 'DELIVERY' as MeetingType,
    restaurantName: '',
    restaurantAddress: '',
    latitude: '',
    longitude: '',
    deliveryFee: '',
    maxParticipants: '',
    bankName: '',
    accountHolder: '',
    accountNumber: '',
  });

  const { data: meData } = useQuery({
    queryKey: ['me'],
    queryFn: () => apiClient.get('/api/auth/me'),
  });
  const savedAddress: string = (meData as any)?.data?.address ?? '';

  const mutation = useMutation({
    mutationFn: (data: any) => apiClient.post('/api/rooms', data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['rooms'] });
      navigation.goBack();
    },
    onError: (e: any) => showAlert('오류', e?.message ?? '방 만들기에 실패했습니다.'),
  });

  function handleSubmit() {
    if (!form.title || !form.restaurantName || !form.restaurantAddress) {
      showAlert('오류', '필수 항목을 모두 입력해주세요.');
      return;
    }
    mutation.mutate({
      ...form,
      latitude: parseFloat(form.latitude) || 37.5,
      longitude: parseFloat(form.longitude) || 127.0,
      deliveryFee: parseInt(form.deliveryFee, 10) || 0,
      maxParticipants: parseInt(form.maxParticipants, 10) || 4,
    });
  }

  function update(key: string, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function fillCurrentLocation() {
    const { status } = await Location.requestForegroundPermissionsAsync();
    if (status !== 'granted') return;
    const loc = await Location.getCurrentPositionAsync({});
    setForm((prev) => ({
      ...prev,
      latitude: String(loc.coords.latitude),
      longitude: String(loc.coords.longitude),
    }));
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.label}>방 제목 *</Text>
      <TextInput style={styles.input} value={form.title} onChangeText={(v) => update('title', v)} placeholder="방 제목" />

      <Text style={styles.label}>모임 유형 *</Text>
      <View style={styles.typeRow}>
        {MEETING_TYPES.map((t) => (
          <TouchableOpacity
            key={t.value}
            style={[styles.typeChip, form.meetingType === t.value && styles.typeChipActive]}
            onPress={() => update('meetingType', t.value)}
          >
            <Text style={[styles.typeText, form.meetingType === t.value && styles.typeTextActive]}>{t.label}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>음식점 이름 *</Text>
      <TextInput style={styles.input} value={form.restaurantName} onChangeText={(v) => update('restaurantName', v)} placeholder="음식점 이름" />

      <Text style={styles.label}>주소 *</Text>
      <TextInput style={styles.input} value={form.restaurantAddress} onChangeText={(v) => update('restaurantAddress', v)} placeholder="주소" />
      <View style={styles.addressBtnRow}>
        <TouchableOpacity style={styles.locationBtn} onPress={fillCurrentLocation}>
          <Text style={styles.locationBtnText}>📍 현재 위치 사용</Text>
        </TouchableOpacity>
        {savedAddress ? (
          <TouchableOpacity style={styles.locationBtn} onPress={() => update('restaurantAddress', savedAddress)}>
            <Text style={styles.locationBtnText}>🏠 내 저장 주소</Text>
          </TouchableOpacity>
        ) : null}
      </View>

      <Text style={styles.label}>배달비 (원)</Text>
      <TextInput style={styles.input} value={form.deliveryFee} onChangeText={(v) => update('deliveryFee', v)} placeholder="3000" keyboardType="numeric" />

      <Text style={styles.label}>최대 인원</Text>
      <TextInput style={styles.input} value={form.maxParticipants} onChangeText={(v) => update('maxParticipants', v)} placeholder="4" keyboardType="numeric" />

      <Text style={styles.label}>은행명</Text>
      <TextInput style={styles.input} value={form.bankName} onChangeText={(v) => update('bankName', v)} placeholder="카카오뱅크" />

      <Text style={styles.label}>예금주</Text>
      <TextInput style={styles.input} value={form.accountHolder} onChangeText={(v) => update('accountHolder', v)} placeholder="홍길동" />

      <Text style={styles.label}>계좌번호</Text>
      <TextInput style={styles.input} value={form.accountNumber} onChangeText={(v) => update('accountNumber', v)} placeholder="1234-5678-9012" keyboardType="numeric" />

      <TouchableOpacity
        style={[styles.button, mutation.isPending && styles.buttonDisabled]}
        onPress={handleSubmit}
        disabled={mutation.isPending}
      >
        <Text style={styles.buttonText}>{mutation.isPending ? '생성 중...' : '방 만들기'}</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { padding: 20, paddingBottom: 40 },
  label: { fontSize: 14, fontWeight: '600', color: '#333', marginBottom: 6, marginTop: 16 },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, fontSize: 15 },
  typeRow: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
  typeChip: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 20, borderWidth: 1, borderColor: '#ddd' },
  typeChipActive: { backgroundColor: '#FF6B35', borderColor: '#FF6B35' },
  typeText: { color: '#666', fontSize: 13 },
  typeTextActive: { color: '#fff', fontWeight: '600' },
  button: { marginTop: 32, backgroundColor: '#FF6B35', borderRadius: 8, padding: 16, alignItems: 'center' },
  buttonDisabled: { opacity: 0.5 },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
  addressBtnRow: { flexDirection: 'row', gap: 12, marginTop: 8 },
  locationBtn: { alignSelf: 'flex-start' },
  locationBtnText: { color: '#FF6B35', fontSize: 13, fontWeight: '600' },
});
