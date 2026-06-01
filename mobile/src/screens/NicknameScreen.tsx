import React, { useState } from 'react';
import { StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useAuth } from '../context/AuthContext';
import { showAlert } from '../utils/alert';
import { RootStackParamList } from '../navigation/AppNavigator';

export function NicknameScreen() {
  const [nickname, setNickname] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();

  async function handleRegister() {
    const trimmed = nickname.trim();
    if (trimmed.length < 2 || trimmed.length > 12) {
      showAlert('오류', '닉네임은 2~12자여야 합니다.');
      return;
    }
    setLoading(true);
    try {
      await register(trimmed);
      navigation.replace('Gender');
    } catch (e: any) {
      showAlert('오류', e?.message ?? '등록에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>닉네임을 입력해주세요</Text>
      <Text style={styles.subtitle}>2~12자, 한번 설정하면 변경할 수 없습니다</Text>
      <TextInput
        style={styles.input}
        value={nickname}
        onChangeText={setNickname}
        placeholder="닉네임 입력"
        maxLength={12}
        autoFocus
      />
      <TouchableOpacity
        style={[styles.button, loading && styles.buttonDisabled]}
        onPress={handleRegister}
        disabled={loading}
      >
        <Text style={styles.buttonText}>{loading ? '등록 중...' : '시작하기'}</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 24 },
  title: { fontSize: 22, fontWeight: 'bold', marginBottom: 8 },
  subtitle: { fontSize: 14, color: '#666', marginBottom: 32 },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, fontSize: 16, marginBottom: 16 },
  button: { backgroundColor: '#FF6B35', borderRadius: 8, padding: 16, alignItems: 'center' },
  buttonDisabled: { opacity: 0.5 },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
});
