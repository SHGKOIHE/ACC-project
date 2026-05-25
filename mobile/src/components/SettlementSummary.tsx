import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

interface Props {
  totalMenuAmount: number;
  deliveryFeePerPerson: number;
  totalAmount: number;
  bankName: string;
  accountHolder: string;
  accountNumber: string;
}

export function SettlementSummary({ totalMenuAmount, deliveryFeePerPerson, totalAmount, bankName, accountHolder, accountNumber }: Props) {
  return (
    <View style={styles.card}>
      <Text style={styles.title}>정산 요약</Text>
      <View style={styles.row}>
        <Text style={styles.label}>메뉴 금액</Text>
        <Text style={styles.value}>{totalMenuAmount.toLocaleString()}원</Text>
      </View>
      <View style={styles.row}>
        <Text style={styles.label}>배달비 (1인당)</Text>
        <Text style={styles.value}>{deliveryFeePerPerson.toLocaleString()}원</Text>
      </View>
      <View style={[styles.row, styles.totalRow]}>
        <Text style={styles.totalLabel}>총 납부액</Text>
        <Text style={styles.totalValue}>{totalAmount.toLocaleString()}원</Text>
      </View>
      <View style={styles.divider} />
      <Text style={styles.accountTitle}>입금 계좌</Text>
      <Text style={styles.account}>{bankName} · {accountHolder}</Text>
      <Text style={styles.accountNumber}>{accountNumber}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card: { backgroundColor: '#fff8f5', borderRadius: 12, padding: 20, marginBottom: 16, borderWidth: 1, borderColor: '#ffe0d0' },
  title: { fontSize: 16, fontWeight: 'bold', marginBottom: 16, color: '#333' },
  row: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  label: { fontSize: 14, color: '#666' },
  value: { fontSize: 14, color: '#333' },
  totalRow: { marginTop: 8, paddingTop: 12, borderTopWidth: 1, borderColor: '#eee' },
  totalLabel: { fontSize: 16, fontWeight: 'bold', color: '#333' },
  totalValue: { fontSize: 16, fontWeight: 'bold', color: '#FF6B35' },
  divider: { height: 1, backgroundColor: '#eee', marginVertical: 16 },
  accountTitle: { fontSize: 13, color: '#999', marginBottom: 4 },
  account: { fontSize: 14, color: '#333' },
  accountNumber: { fontSize: 18, fontWeight: 'bold', color: '#333', marginTop: 4 },
});
