import React, { useEffect, useState } from 'react';
import {
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../api/client';
import { useAuth } from '../context/AuthContext';

const GENDERS = [
  { label: '남성', value: 'MALE' },
  { label: '여성', value: 'FEMALE' },
];

export function ProfileScreen() {
  const { nickname, logout } = useAuth();
  const qc = useQueryClient();
  const [address, setAddress] = useState('');
  const [selectedGender, setSelectedGender] = useState<string | null>(null);
  const [initialized, setInitialized] = useState(false);

  const { data } = useQuery({
    queryKey: ['me'],
    queryFn: () => apiClient.get('/api/auth/me'),
  });

  useEffect(() => {
    if (data && !initialized) {
      const me = (data as any)?.data;
      setAddress(me?.address ?? '');
      setSelectedGender(me?.gender ?? null);
      setInitialized(true);
    }
  }, [data]);

  const me: any = (data as any)?.data;

  const updateMutation = useMutation({
    mutationFn: (body: any) => apiClient.put('/api/auth/me', body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['me'] });
      Alert.alert('저장', '프로필이 업데이트되었습니다.');
    },
    onError: () => Alert.alert('오류', '저장에 실패했습니다.'),
  });

  const withdrawMutation = useMutation({
    mutationFn: () => apiClient.delete('/api/auth/me'),
    onSuccess: async () => {
      await logout();
    },
    onError: () => Alert.alert('오류', '탈퇴에 실패했습니다.'),
  });

  function handleSave() {
    updateMutation.mutate({ gender: selectedGender, address: address.trim() || null });
  }

  function handleWithdraw() {
    Alert.alert(
      '회원 탈퇴',
      '정말 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.',
      [
        { text: '취소', style: 'cancel' },
        { text: '탈퇴', style: 'destructive', onPress: () => withdrawMutation.mutate() },
      ]
    );
  }


  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.heading}>프로필</Text>

      <View style={styles.infoBox}>
        <Text style={styles.infoLabel}>닉네임</Text>
        <Text style={styles.infoValue}>{me?.nickname ?? nickname ?? '-'}</Text>
      </View>

      {me?.email ? (
        <View style={styles.infoBox}>
          <Text style={styles.infoLabel}>이메일</Text>
          <Text style={styles.infoValue}>{me.email} {me.emailVerified ? '✓' : ''}</Text>
        </View>
      ) : null}

      <Text style={styles.label}>성별</Text>
      <View style={styles.genderRow}>
        {GENDERS.map((g) => (
          <TouchableOpacity
            key={String(g.value)}
            style={[styles.genderChip, selectedGender === g.value && styles.genderChipActive]}
            onPress={() => setSelectedGender(g.value)}
          >
            <Text style={[styles.genderText, selectedGender === g.value && styles.genderTextActive]}>
              {g.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>주소</Text>
      <TextInput
        style={styles.input}
        value={address}
        onChangeText={setAddress}
        placeholder="주소를 입력하세요"
      />

      <TouchableOpacity
        style={[styles.saveBtn, updateMutation.isPending && styles.btnDisabled]}
        onPress={handleSave}
        disabled={updateMutation.isPending}
      >
        <Text style={styles.saveBtnText}>{updateMutation.isPending ? '저장 중...' : '저장'}</Text>
      </TouchableOpacity>

      <TouchableOpacity style={styles.withdrawBtn} onPress={handleWithdraw}>
        <Text style={styles.withdrawBtnText}>회원 탈퇴</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { padding: 20, paddingBottom: 40 },
  heading: { fontSize: 22, fontWeight: 'bold', marginBottom: 24, color: '#1a1a1a' },
  infoBox: { backgroundColor: '#f8f8f8', borderRadius: 8, padding: 14, marginBottom: 12 },
  infoLabel: { fontSize: 12, color: '#999', marginBottom: 2 },
  infoValue: { fontSize: 15, color: '#333', fontWeight: '500' },
  label: { fontSize: 14, fontWeight: '600', color: '#333', marginBottom: 8, marginTop: 16 },
  genderRow: { flexDirection: 'row', gap: 8 },
  genderChip: { paddingHorizontal: 16, paddingVertical: 8, borderRadius: 20, borderWidth: 1, borderColor: '#ddd' },
  genderChipActive: { backgroundColor: '#FF6B35', borderColor: '#FF6B35' },
  genderText: { fontSize: 13, color: '#666' },
  genderTextActive: { color: '#fff', fontWeight: '600' },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, fontSize: 15 },
  saveBtn: { marginTop: 24, backgroundColor: '#FF6B35', borderRadius: 8, padding: 16, alignItems: 'center' },
  btnDisabled: { opacity: 0.5 },
  saveBtnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
  withdrawBtn: { marginTop: 12, borderWidth: 1, borderColor: '#e74c3c', borderRadius: 8, padding: 14, alignItems: 'center' },
  withdrawBtnText: { color: '#e74c3c', fontSize: 15, fontWeight: '600' },
});
