import React, { useState } from 'react';
import { StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { apiClient } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { showAlert } from '../utils/alert';
import { RootStackParamList } from '../navigation/AppNavigator';

type Step = 'input' | 'verify';

export function EmailVerifyScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { completeOnboarding } = useAuth();
  const [step, setStep] = useState<Step>('input');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSendCode() {
    if (!email.trim()) {
      showAlert('오류', '이메일을 입력해주세요.');
      return;
    }
    setLoading(true);
    try {
      await apiClient.post('/api/auth/email/send-code', { email: email.trim() });
      setStep('verify');
    } catch (e: any) {
      showAlert('오류', e?.message ?? '인증코드 전송에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }

  async function handleVerify() {
    if (!code.trim()) {
      showAlert('오류', '인증코드를 입력해주세요.');
      return;
    }
    setLoading(true);
    try {
      await apiClient.post('/api/auth/email/verify', { email: email.trim(), code: code.trim() });
      completeOnboarding();
      navigation.replace('Main');
    } catch (e: any) {
      showAlert('오류', e?.message ?? '인증에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }

  function handleSkip() {
    completeOnboarding();
    navigation.replace('Main');
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>이메일 인증</Text>
      <Text style={styles.subtitle}>이메일을 인증하면 계정을 안전하게 보호할 수 있어요</Text>

      {step === 'input' ? (
        <>
          <TextInput
            style={styles.input}
            value={email}
            onChangeText={setEmail}
            placeholder="이메일 주소 입력"
            keyboardType="email-address"
            autoCapitalize="none"
            autoCorrect={false}
          />
          <TouchableOpacity
            style={[styles.button, loading && styles.buttonDisabled]}
            onPress={handleSendCode}
            disabled={loading}
          >
            <Text style={styles.buttonText}>{loading ? '전송 중...' : '인증코드 전송'}</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.skipBtn} onPress={handleSkip}>
            <Text style={styles.skipText}>나중에 하기</Text>
          </TouchableOpacity>
        </>
      ) : (
        <>
          <Text style={styles.emailLabel}>{email}</Text>
          <TextInput
            style={styles.input}
            value={code}
            onChangeText={setCode}
            placeholder="6자리 인증코드 입력"
            keyboardType="numeric"
            maxLength={6}
            autoFocus
          />
          <TouchableOpacity
            style={[styles.button, loading && styles.buttonDisabled]}
            onPress={handleVerify}
            disabled={loading}
          >
            <Text style={styles.buttonText}>{loading ? '인증 중...' : '인증 완료'}</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.skipBtn} onPress={handleSkip}>
            <Text style={styles.skipText}>나중에 하기</Text>
          </TouchableOpacity>
        </>
      )}

    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 24, backgroundColor: '#fff' },
  title: { fontSize: 22, fontWeight: 'bold', color: '#1a1a1a', marginBottom: 8 },
  subtitle: { fontSize: 14, color: '#888', marginBottom: 32 },
  emailLabel: { fontSize: 14, color: '#FF6B35', marginBottom: 12, fontWeight: '600' },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    marginBottom: 16,
    color: '#333',
  },
  button: { backgroundColor: '#FF6B35', borderRadius: 10, padding: 16, alignItems: 'center', marginBottom: 12 },
  buttonDisabled: { opacity: 0.5 },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
  skipBtn: { alignItems: 'center', marginTop: 8 },
  skipText: { color: '#aaa', fontSize: 14, textAlign: 'center' },
});
