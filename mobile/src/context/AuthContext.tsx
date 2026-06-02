import React, { createContext, useContext, useEffect, useState } from 'react';
import * as Crypto from 'expo-crypto';
import * as Notifications from 'expo-notifications';
import { apiClient } from '../api/client';
import { getItem, setItem, removeItem } from '../utils/storage';

interface AuthState {
  deviceToken: string | null;
  memberId: string | null;
  nickname: string | null;
  isLoading: boolean;
  isNewUser: boolean;
  register: (nickname: string) => Promise<void>;
  updateFcmToken: (fcmToken: string) => Promise<void>;
  completeOnboarding: () => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [deviceToken, setDeviceToken] = useState<string | null>(null);
  const [memberId, setMemberId] = useState<string | null>(null);
  const [nickname, setNickname] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isNewUser, setIsNewUser] = useState(false);

  useEffect(() => {
    initDeviceToken();
  }, []);

  async function initDeviceToken() {
    try {
      let token: string | null = await getItem('deviceToken');
      if (!token) {
        token = await Crypto.randomUUID();
        await setItem('deviceToken', token);
      }
      let activeToken = token;
      setDeviceToken(token);

      const stored = await getItem('memberInfo');
      if (stored) {
        const raw = JSON.parse(stored);
        // 이전 버전에서 {success, data:{id,...}} 형태로 저장된 경우 복구
        const info = raw.data ?? raw;
        if (info?.id) {
          if (info.deviceToken && info.deviceToken !== token) {
            activeToken = info.deviceToken;
            await setItem('deviceToken', activeToken);
            setDeviceToken(activeToken);
          }
          const valid = await verifyMember(activeToken);
          if (valid) {
            setMemberId(info.id);
            setNickname(info.nickname);
            // 깨진 형식이면 올바른 형식으로 재저장
            if (raw.data) await setItem('memberInfo', JSON.stringify(info));
            await registerPushToken();
          } else {
            await removeItem('memberInfo');
          }
        } else {
          await removeItem('memberInfo');
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

  async function registerPushToken() {
    try {
      const { status } = await Notifications.requestPermissionsAsync();
      if (status !== 'granted') return;
      const token = (await Notifications.getExpoPushTokenAsync()).data;
      try {
        await apiClient.put('/api/auth/fcm-token', { fcmToken: token });
      } catch {
        // silent
      }
    } catch {
      // silent - permissions or token fetch failed
    }
  }

  async function register(nick: string) {
    try {
      const activeDeviceToken = deviceToken ?? await getItem('deviceToken');
      const res: any = await apiClient.post('/api/auth/register', {
        nickname: nick,
        deviceToken: activeDeviceToken,
      });
      const member = res.data;
      if (member.deviceToken) {
        await setItem('deviceToken', member.deviceToken);
        setDeviceToken(member.deviceToken);
      }
      setMemberId(member.id);
      setNickname(member.nickname);
      setIsNewUser(true);
      await setItem('memberInfo', JSON.stringify(member));
      await registerPushToken();
    } catch (e) {
      setMemberId(null);
      setNickname(null);
      throw e;
    }
  }

  async function updateFcmToken(fcmToken: string) {
    await apiClient.put('/api/auth/fcm-token', { fcmToken });
  }

  function completeOnboarding() {
    setIsNewUser(false);
  }

  async function logout() {
    await removeItem('memberInfo');
    setMemberId(null);
    setNickname(null);
    setIsNewUser(false);
    // deviceToken is a stable device identifier — keep it so register() works after logout
  }

  return (
    <AuthContext.Provider value={{ deviceToken, memberId, nickname, isLoading, isNewUser, register, updateFcmToken, completeOnboarding, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
