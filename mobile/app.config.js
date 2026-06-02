const { existsSync } = require('fs');
const { resolve } = require('path');

const iosGoogleServices = resolve(__dirname, './GoogleService-Info.plist');
const androidGoogleServices = resolve(__dirname, './google-services.json');

module.exports = {
  expo: {
    name: '같이먹자',
    slug: 'foodgroup',
    version: '1.0.0',
    orientation: 'default',
    userInterfaceStyle: 'light',
    assetBundlePatterns: ['**/*'],
    ios: {
      supportsTablet: false,
      bundleIdentifier: 'com.foodgroup.mobile',
      ...(existsSync(iosGoogleServices) && { googleServicesFile: './GoogleService-Info.plist' }),
    },
    android: {
      adaptiveIcon: {
        foregroundImage: './assets/adaptive-icon.png',
        backgroundColor: '#ffffff',
      },
      package: 'com.foodgroup.mobile',
      ...(existsSync(androidGoogleServices) && { googleServicesFile: './google-services.json' }),
    },
    plugins: [
      'expo-dev-client',
      [
        'expo-secure-store',
        { faceIDPermission: 'Allow $(PRODUCT_NAME) to use Face ID.' },
      ],
      [
        'expo-notifications',
        {
          icon: './assets/adaptive-icon.png',
          color: '#FF6B35',
        },
      ],
      [
        'expo-location',
        { locationAlwaysAndWhenInUsePermission: '주변 음식점을 찾기 위해 위치 정보가 필요합니다.' },
      ],
    ],
    extra: {
      apiBaseUrl: process.env.EXPO_PUBLIC_API_BASE_URL,
      eas: { projectId: process.env.EAS_PROJECT_ID ?? 'bf62240d-7fb1-463c-b4cc-438a5694213e' },
    },
  },
};
