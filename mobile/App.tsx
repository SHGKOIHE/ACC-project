import React from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from './src/api/queryClient';
import { AuthProvider } from './src/context/AuthContext';
import { AppNavigator } from './src/navigation/AppNavigator';

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <AppNavigator />
      </AuthProvider>
    </QueryClientProvider>
  );
}
