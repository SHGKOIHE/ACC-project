import React, { useState } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { apiClient } from '../api/client';
import { RootStackParamList } from '../navigation/AppNavigator';

type Gender = 'MALE' | 'FEMALE' | null;

const GENDER_OPTIONS: { label: string; value: Gender }[] = [
  { label: '남성', value: 'MALE' },
  { label: '여성', value: 'FEMALE' },
  { label: '선택 안함', value: null },
];

export function GenderScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const [selected, setSelected] = useState<Gender>(null);
  const [loading, setLoading] = useState(false);

  async function handleNext() {
    setLoading(true);
    try {
      await apiClient.put('/api/auth/me', { gender: selected });
    } catch {
      // silent - non-blocking
    } finally {
      setLoading(false);
    }
    navigation.navigate('EmailVerify');
  }

  function handleSkip() {
    navigation.navigate('EmailVerify');
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>성별을 선택해주세요</Text>
      <Text style={styles.subtitle}>더 나은 추천을 위해 활용됩니다</Text>

      <View style={styles.options}>
        {GENDER_OPTIONS.map((opt) => (
          <TouchableOpacity
            key={String(opt.value)}
            style={[styles.option, selected === opt.value && styles.optionActive]}
            onPress={() => setSelected(opt.value)}
          >
            <Text style={[styles.optionText, selected === opt.value && styles.optionTextActive]}>
              {opt.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <TouchableOpacity
        style={[styles.button, loading && styles.buttonDisabled]}
        onPress={handleNext}
        disabled={loading}
      >
        <Text style={styles.buttonText}>{loading ? '저장 중...' : '다음'}</Text>
      </TouchableOpacity>

      <TouchableOpacity style={styles.skipBtn} onPress={handleSkip}>
        <Text style={styles.skipText}>건너뛰기</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 24, backgroundColor: '#fff' },
  title: { fontSize: 22, fontWeight: 'bold', color: '#1a1a1a', marginBottom: 8 },
  subtitle: { fontSize: 14, color: '#888', marginBottom: 40 },
  options: { gap: 12, marginBottom: 32 },
  option: {
    padding: 16,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 10,
    alignItems: 'center',
  },
  optionActive: { borderColor: '#FF6B35', backgroundColor: '#fff8f5' },
  optionText: { fontSize: 16, color: '#555' },
  optionTextActive: { color: '#FF6B35', fontWeight: '600' },
  button: { backgroundColor: '#FF6B35', borderRadius: 10, padding: 16, alignItems: 'center', marginBottom: 12 },
  buttonDisabled: { opacity: 0.5 },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
  skipBtn: { alignItems: 'center', padding: 8 },
  skipText: { color: '#aaa', fontSize: 14 },
});
