import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { ActivityIndicator, Text, View } from 'react-native';
import { useAuth } from '../context/AuthContext';
import { NicknameScreen } from '../screens/NicknameScreen';
import { RoomListScreen } from '../screens/RoomListScreen';
import { CreateRoomScreen } from '../screens/CreateRoomScreen';
import { RoomDetailScreen } from '../screens/RoomDetailScreen';
import { SettlementScreen } from '../screens/SettlementScreen';
import { ChatScreen } from '../screens/ChatScreen';
import { AiRecommendScreen } from '../screens/AiRecommendScreen';
import { GenderScreen } from '../screens/GenderScreen';
import { EmailVerifyScreen } from '../screens/EmailVerifyScreen';
import { MapScreen } from '../screens/MapScreen';
import { ProfileScreen } from '../screens/ProfileScreen';

export type RootStackParamList = {
  Nickname: undefined;
  Gender: undefined;
  EmailVerify: undefined;
  Main: undefined;
  CreateRoom: undefined;
  RoomDetail: { roomId: string };
  Settlement: { roomId: string };
  Chat: { roomId: string; roomTitle: string };
};

export type MainTabParamList = {
  RoomList: undefined;
  AiRecommend: undefined;
  Map: undefined;
  Profile: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator<MainTabParamList>();

function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: '#FF6B35',
        tabBarInactiveTintColor: '#aaa',
        tabBarLabel: ({ color }) => {
          let label = '같이먹자';
          if (route.name === 'AiRecommend') label = 'AI 추천';
          else if (route.name === 'Map') label = '지도';
          else if (route.name === 'Profile') label = '프로필';
          return <Text style={{ fontSize: 11, color }}>{label}</Text>;
        },
        tabBarIcon: ({ color, size }) => {
          let icon = '🍽';
          if (route.name === 'AiRecommend') icon = '✨';
          else if (route.name === 'Map') icon = '🗺';
          else if (route.name === 'Profile') icon = '👤';
          return <Text style={{ fontSize: size - 4, color }}>{icon}</Text>;
        },
      })}
    >
      <Tab.Screen name="RoomList" component={RoomListScreen} options={{ title: '같이먹자' }} />
      <Tab.Screen name="AiRecommend" component={AiRecommendScreen} options={{ title: 'AI 추천' }} />
      <Tab.Screen name="Map" component={MapScreen} options={{ title: '지도' }} />
      <Tab.Screen name="Profile" component={ProfileScreen} options={{ title: '프로필' }} />
    </Tab.Navigator>
  );
}

export function AppNavigator() {
  const { memberId, isLoading, isNewUser } = useAuth();

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
            {isNewUser && (
              <>
                <Stack.Screen name="Gender" component={GenderScreen} options={{ title: '성별 선택', headerBackVisible: false }} />
                <Stack.Screen name="EmailVerify" component={EmailVerifyScreen} options={{ title: '이메일 인증', headerBackVisible: false }} />
              </>
            )}
            <Stack.Screen name="Main" component={MainTabs} options={{ headerShown: false }} />
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
