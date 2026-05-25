import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

interface Props {
  menuName: string;
  price: number;
}

export function OrderItemRow({ menuName, price }: Props) {
  return (
    <View style={styles.row}>
      <Text style={styles.name}>{menuName}</Text>
      <Text style={styles.price}>{price.toLocaleString()}원</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 10, borderBottomWidth: 1, borderColor: '#f0f0f0' },
  name: { fontSize: 15, color: '#333' },
  price: { fontSize: 15, fontWeight: '600', color: '#333' },
});
