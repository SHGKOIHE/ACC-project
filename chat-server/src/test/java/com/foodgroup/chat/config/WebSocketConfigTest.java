package com.foodgroup.chat.config;

import com.foodgroup.chat.auth.JwtChannelInterceptor;
import com.foodgroup.chat.auth.LightsailIpHandshakeInterceptor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    @Test
    void registersNativeStompEndpointAtWsNative() {
        JwtChannelInterceptor jwtChannelInterceptor = mock(JwtChannelInterceptor.class);
        LightsailIpHandshakeInterceptor handshakeInterceptor = mock(LightsailIpHandshakeInterceptor.class);
        WebSocketConfig config = new WebSocketConfig(jwtChannelInterceptor, handshakeInterceptor);
        ReflectionTestUtils.setField(config, "allowedOrigins", "*");

        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws-native")).thenReturn(registration);
        when(registration.addInterceptors(any())).thenReturn(registration);
        when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

        config.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/ws-native");
        ArgumentCaptor<LightsailIpHandshakeInterceptor> interceptorCaptor = ArgumentCaptor.forClass(LightsailIpHandshakeInterceptor.class);
        verify(registration).addInterceptors(interceptorCaptor.capture());
        verify(registration).setAllowedOriginPatterns("*");
        assertThat(interceptorCaptor.getValue()).isSameAs(handshakeInterceptor);
    }

    @Test
    void installsJwtInterceptorOnInboundChannel() {
        JwtChannelInterceptor jwtChannelInterceptor = mock(JwtChannelInterceptor.class);
        LightsailIpHandshakeInterceptor handshakeInterceptor = mock(LightsailIpHandshakeInterceptor.class);
        WebSocketConfig config = new WebSocketConfig(jwtChannelInterceptor, handshakeInterceptor);
        ChannelRegistration registration = mock(ChannelRegistration.class);

        config.configureClientInboundChannel(registration);

        verify(registration).interceptors(jwtChannelInterceptor);
    }
}
