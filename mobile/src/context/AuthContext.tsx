import React, { createContext, useContext, useEffect, useState } from 'react';
import * as Crypto from 'expo-crypto';
import { apiClient } from '../api/client';
import { getItem, setItem } from '../utils/storage';

interface AuthState {
  deviceToken: string | null;
  memberId: number | null;
  nickname: string | null;
  isLoading: boolean;
  register: (nickname: string) => Promise<void>;
  updateFcmToken: (fcmToken: string) => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [deviceToken, setDeviceToken] = useState<string | null>(null);
  const [memberId, setMemberId] = useState<number | null>(null);
  const [nickname, setNickname] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    initDeviceToken();
  }, []);

  async function initDeviceToken() {
    try {
      let token = await getItem('deviceToken');
      if (!token) {
        token = await Crypto.randomUUID();
        await setItem('deviceToken', token);
      }
      setDeviceToken(token);

      const stored = await getItem('memberInfo');
      if (stored) {
        const info = JSON.parse(stored);
        // DB에 해당 회원이 실제로 존재하는지 확인
        const valid = await verifyMember(token);
        if (valid) {
          setMemberId(info.id);
          setNickname(info.nickname);
        } else {
          await setItem('memberInfo', '');
        }
      }
    } finally {
      setIsLoading(false);
    }
  }

  async function verifyMember(token: string): Promise<boolean> {
    try {
      const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
      const res = await fetch(`${API_BASE_URL}/api/auth/me`, {
        headers: { 'X-Device-Token': token },
      });
      return res.ok;
    } catch {
      return true; // 네트워크 오류 시 오프라인으로 간주, 기존 상태 유지
    }
  }

  async function register(nick: string) {
    try {
      const res: any = await apiClient.post('/api/auth/register', {
        nickname: nick,
        deviceToken,
      });
      const member = res.data;
      setMemberId(member.id);
      setNickname(member.nickname);
      await setItem('memberInfo', JSON.stringify(member));
    } catch (e) {
      setMemberId(null);
      setNickname(null);
      throw e;
    }
  }

  async function updateFcmToken(fcmToken: string) {
    await apiClient.put('/api/auth/fcm-token', { fcmToken });
  }

  return (
    <AuthContext.Provider value={{ deviceToken, memberId, nickname, isLoading, register, updateFcmToken }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
