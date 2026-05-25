import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { ActivityIndicator, View } from 'react-native';
import { useAuth } from '../context/AuthContext';
import { NicknameScreen } from '../screens/NicknameScreen';
import { RoomListScreen } from '../screens/RoomListScreen';
import { CreateRoomScreen } from '../screens/CreateRoomScreen';
import { RoomDetailScreen } from '../screens/RoomDetailScreen';
import { SettlementScreen } from '../screens/SettlementScreen';
import { ChatScreen } from '../screens/ChatScreen';

export type RootStackParamList = {
  Nickname: undefined;
  RoomList: undefined;
  CreateRoom: undefined;
  RoomDetail: { roomId: number };
  Settlement: { roomId: number };
  Chat: { roomId: number; roomTitle: string };
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export function AppNavigator() {
  const { memberId, isLoading } = useAuth();

  if (isLoading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: true }}>
        {memberId == null ? (
          <Stack.Screen name="Nickname" component={NicknameScreen} options={{ title: '닉네임 등록' }} />
        ) : (
          <>
            <Stack.Screen name="RoomList" component={RoomListScreen} options={{ title: '같이먹자' }} />
            <Stack.Screen name="CreateRoom" component={CreateRoomScreen} options={{ title: '방 만들기' }} />
            <Stack.Screen name="RoomDetail" component={RoomDetailScreen} options={{ title: '방 상세' }} />
            <Stack.Screen name="Settlement" component={SettlementScreen} options={{ title: '정산 내역' }} />
            <Stack.Screen name="Chat" component={ChatScreen} options={({ route }) => ({ title: route.params.roomTitle })} />
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
