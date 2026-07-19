package com.foodgroup.chat.auth;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * IP 화이트리스트 제거 이후의 동작을 검증한다.
 * 핸드셰이크 허용 기준은 소스 IP가 아니라 Redis에 유효한 디바이스 토큰이 존재하는지 여부다.
 */
class LightsailIpHandshakeInterceptorTest {

    @Test
    void acceptsHandshakeWhenDeviceTokenExistsInRedis() {
        RedisMocks redis = redisReturning("member-1");
        LightsailIpHandshakeInterceptor interceptor = new LightsailIpHandshakeInterceptor(
                redis.template(), "device-token:"
        );
        ServerHttpRequest request = requestWithHeaders(Map.of(
                "X-Forwarded-For", "100.64.11.88, 10.0.0.1",
                "X-Device-Token", "device-1"
        ));
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(request, null, mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes).containsEntry(JwtChannelInterceptor.SESSION_MEMBER_ID, "member-1")
                .containsEntry("deviceToken", "device-1");
        verify(redis.values()).get("device-token:device-1");
    }

    @Test
    void rejectsHandshakeWhenDeviceTokenIsMissing() {
        RedisMocks redis = redisReturning("member-1");
        LightsailIpHandshakeInterceptor interceptor = new LightsailIpHandshakeInterceptor(
                redis.template(), "device-token:"
        );
        ServerHttpRequest request = requestWithHeaders(Map.of("X-Forwarded-For", "100.64.11.88"));

        boolean allowed = interceptor.beforeHandshake(request, null, mock(WebSocketHandler.class), new HashMap<>());

        assertThat(allowed).isFalse();
        verifyNoInteractions(redis.values());
    }

    @Test
    void rejectsHandshakeWhenDeviceTokenDoesNotExistInRedis() {
        RedisMocks redis = redisReturning(null);
        LightsailIpHandshakeInterceptor interceptor = new LightsailIpHandshakeInterceptor(
                redis.template(), "device-token:"
        );
        ServerHttpRequest request = requestWithHeaders(Map.of(
                "X-Real-IP", "43.201.33.167",
                "X-Device-Token", "unknown-device"
        ));

        boolean allowed = interceptor.beforeHandshake(request, null, mock(WebSocketHandler.class), new HashMap<>());

        assertThat(allowed).isFalse();
        verify(redis.values()).get("device-token:unknown-device");
    }

    @Test
    void acceptsHandshakeRegardlessOfSourceIpWhenTokenIsValid() {
        RedisMocks redis = redisReturning("member-1");
        LightsailIpHandshakeInterceptor interceptor = new LightsailIpHandshakeInterceptor(
                redis.template(), "device-token:"
        );
        ServerHttpRequest request = requestWithRemoteAddressAndToken("203.0.113.10", "device-1");

        boolean allowed = interceptor.beforeHandshake(request, null, mock(WebSocketHandler.class), new HashMap<>());

        assertThat(allowed).isTrue();
        verify(redis.values()).get("device-token:device-1");
    }

    private ServerHttpRequest requestWithHeaders(Map<String, String> values) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        values.forEach(headers::add);
        when(request.getHeaders()).thenReturn(headers);
        return request;
    }

    private ServerHttpRequest requestWithRemoteAddressAndToken(String host, String deviceToken) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Device-Token", deviceToken);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress(host, 12345));
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
