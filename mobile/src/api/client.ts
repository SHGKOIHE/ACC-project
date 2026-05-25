import axios from 'axios';
import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

async function getDeviceToken(): Promise<string | null> {
  if (Platform.OS === 'web') return localStorage.getItem('deviceToken');
  return SecureStore.getItemAsync('deviceToken');
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use(async (config) => {
  const token = await getDeviceToken();
  if (token) {
    config.headers['X-Device-Token'] = token;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(error.response?.data ?? error),
);
