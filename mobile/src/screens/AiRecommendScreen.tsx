import React, { useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { useMutation } from '@tanstack/react-query';
import { apiClient } from '../api/client';

const CATEGORIES = ['전체', '한식', '중식', '일식', '양식', '분식', '치킨', '피자', '버거'];

interface RecommendItem {
  rank: number;
  restaurantName: string;
  score: number;
  reason: string;
}

interface RecommendResponse {
  recommendations: RecommendItem[];
  explanation: string;
}

export function AiRecommendScreen() {
  const [selectedCategory, setSelectedCategory] = useState('전체');
  const [maxFee, setMaxFee] = useState('');
  const [userMessage, setUserMessage] = useState('');

  const mutation = useMutation({
    mutationFn: () =>
      apiClient.post('/api/recommend', {
        latitude: 37.5665,
        longitude: 126.978,
        category: selectedCategory === '전체' ? null : selectedCategory,
        maxDeliveryFee: maxFee ? parseInt(maxFee, 10) : null,
        userMessage: userMessage || null,
      }),
  });

  const result = (mutation.data as any)?.data as RecommendResponse | undefined;

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Text style={styles.heading}>AI 음식 추천</Text>
      <Text style={styles.subtitle}>취향과 조건을 입력하면 AI가 음식점을 추천해드려요</Text>

      <View style={styles.section}>
        <Text style={styles.label}>무엇이 드시고 싶으신가요?</Text>
        <TextInput
          style={styles.textArea}
          value={userMessage}
          onChangeText={setUserMessage}
          placeholder="예: 친구들이랑 시켜먹을 건데 매운 거 좋아해요. 배달비 저렴한 곳으로 추천해주세요"
          multiline
          numberOfLines={4}
          textAlignVertical="top"
        />
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>카테고리</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          <View style={styles.chips}>
            {CATEGORIES.map((c) => (
              <TouchableOpacity
                key={c}
                style={[styles.chip, selectedCategory === c && styles.chipActive]}
                onPress={() => setSelectedCategory(c)}
              >
                <Text style={[styles.chipText, selectedCategory === c && styles.chipTextActive]}>
                  {c}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </ScrollView>
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>최대 배달비 (원)</Text>
        <TextInput
          style={styles.input}
          value={maxFee}
          onChangeText={setMaxFee}
          placeholder="예: 3000"
          keyboardType="numeric"
        />
      </View>

      <TouchableOpacity
        style={[styles.button, mutation.isPending && styles.buttonDisabled]}
        onPress={() => mutation.mutate()}
        disabled={mutation.isPending}
      >
        {mutation.isPending ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>AI 추천 받기</Text>
        )}
      </TouchableOpacity>

      {mutation.isError && (
        <Text style={styles.errorText}>추천을 가져오지 못했습니다. 다시 시도해주세요.</Text>
      )}

      {result && (
        <View style={styles.results}>
          {result.explanation ? (
            <Text style={styles.explanation}>{result.explanation}</Text>
          ) : null}
          {result.recommendations.length === 0 ? (
            <Text style={styles.emptyText}>추천 결과가 없습니다.</Text>
          ) : (
            result.recommendations.map((item) => (
              <View key={item.rank} style={styles.card}>
                <View style={styles.cardHeader}>
                  <View style={styles.rankBadge}>
                    <Text style={styles.rankText}>{item.rank}</Text>
                  </View>
                  <Text style={styles.restaurantName}>{item.restaurantName}</Text>
                  <Text style={styles.score}>점수 {item.score}</Text>
                </View>
                <Text style={styles.reason}>{item.reason}</Text>
              </View>
            ))
          )}
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { padding: 20 },
  heading: { fontSize: 22, fontWeight: 'bold', color: '#1a1a1a', marginBottom: 4 },
  subtitle: { fontSize: 14, color: '#888', marginBottom: 24 },
  section: { marginBottom: 20 },
  label: { fontSize: 15, fontWeight: '600', marginBottom: 10, color: '#333' },
  chips: { flexDirection: 'row', gap: 8 },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#ddd',
    backgroundColor: '#fff',
  },
  chipActive: { backgroundColor: '#FF6B35', borderColor: '#FF6B35' },
  chipText: { fontSize: 13, color: '#555' },
  chipTextActive: { color: '#fff', fontWeight: '600' },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 14,
    color: '#333',
  },
  textArea: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 14,
    color: '#333',
    height: 110,
    textAlignVertical: 'top',
  },
  button: {
    backgroundColor: '#FF6B35',
    borderRadius: 10,
    padding: 15,
    alignItems: 'center',
    marginBottom: 20,
  },
  buttonDisabled: { opacity: 0.6 },
  buttonText: { color: '#fff', fontWeight: 'bold', fontSize: 16 },
  errorText: { color: '#e74c3c', textAlign: 'center', marginBottom: 12 },
  results: { marginTop: 8 },
  explanation: {
    fontSize: 14,
    color: '#555',
    backgroundColor: '#fff8f0',
    padding: 14,
    borderRadius: 8,
    marginBottom: 16,
    lineHeight: 20,
  },
  emptyText: { textAlign: 'center', color: '#aaa', fontSize: 14, marginTop: 20 },
  card: {
    backgroundColor: '#fafafa',
    borderRadius: 10,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#f0f0f0',
  },
  cardHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: 8, gap: 10 },
  rankBadge: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: '#FF6B35',
    justifyContent: 'center',
    alignItems: 'center',
  },
  rankText: { color: '#fff', fontWeight: 'bold', fontSize: 13 },
  restaurantName: { flex: 1, fontSize: 16, fontWeight: '600', color: '#222' },
  score: { fontSize: 13, color: '#FF6B35', fontWeight: '600' },
  reason: { fontSize: 13, color: '#666', lineHeight: 18 },
});
