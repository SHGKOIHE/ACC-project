package com.foodgroup.chat.auth;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LightsailIpHandshakeInterceptorTest {

    @Test
    void preAuthenticatesWhenDeviceTokenHeaderPresentAndValid() {
        RedisMocks redis = redisReturning("member-1");
        LightsailIpHandshakeInterceptor interceptor = new LightsailIpHandshakeInterceptor(redis.template(), "device-token:");
        ServerHttpRequest request = requestWithHeaders(Map.of("X-Device-Token", "device-1"));
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(request, null, mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes).containsEntry(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1")
                .containsEntry("deviceToken", "device-1");
        verify(redis.values()).get("device-token:device-1");
    }

    // Standard WebSocket clients (browsers, React Native's WebSocket) cannot set custom HTTP
    // headers on the handshake request, so this is the common case: no header at all. The
    // handshake must still be allowed — real auth happens at STOMP CONNECT.
    @Test
    void allowsHandshakeWhenDeviceTokenHeaderIsMissing() {
        RedisMocks redis = redisReturning("member-1");
        LightsailIpHandshakeInterceptor interceptor = new LightsailIpHandshakeInterceptor(redis.template(), "device-token:");
        ServerHttpRequest request = requestWithHeaders(Map.of());
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(request, null, mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes).isEmpty();
        verifyNoInteractions(redis.values());
    }

    @Test
    void allowsHandshakeButSkipsPreAuthWhenDeviceTokenNotFoundInRedis() {
        RedisMocks redis = redisReturning(null);
        LightsailIpHandshakeInterceptor interceptor = new LightsailIpHandshakeInterceptor(redis.template(), "device-token:");
        ServerHttpRequest request = requestWithHeaders(Map.of("X-Device-Token", "unknown-device"));
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(request, null, mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes).isEmpty();
        verify(redis.values()).get("device-token:unknown-device");
    }

    private ServerHttpRequest requestWithHeaders(Map<String, String> values) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        values.forEach(headers::add);
        when(request.getHeaders()).thenReturn(headers);
        return request;
    }

    @SuppressWarnings("unchecked")
    private RedisMocks redisReturning(String memberId) {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get(org.mockito.ArgumentMatchers.anyString())).thenReturn(memberId);
        return new RedisMocks(redisTemplate, values);
    }

    private record RedisMocks(StringRedisTemplate template, ValueOperations<String, String> values) {
    }
}
