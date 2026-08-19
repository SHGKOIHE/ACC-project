package com.foodgroup.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DeviceTokenServiceTest {

    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> stubValueOps(StringRedisTemplate template) {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        return values;
    }

    private DeviceTokenService newService(StringRedisTemplate template) {
        DeviceTokenService service = new DeviceTokenService(template);
        ReflectionTestUtils.setField(service, "keyPrefix", "device-token:");
        ReflectionTestUtils.setField(service, "ttlHours", 24L);
        return service;
    }

    @Test
    void resolveMemberIdSlidesTheRedisTtlForwardOnEachSuccessfulLookup() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = stubValueOps(template);
        when(values.get("device-token:tok-1")).thenReturn("member-1");
        DeviceTokenService service = newService(template);

        Optional<String> resolved = service.resolveMemberId("tok-1");

        assertThat(resolved).contains("member-1");
        verify(template).expire("device-token:tok-1", Duration.ofHours(24));
    }

    @Test
    void resolveMemberIdDoesNotTouchTtlWhenTokenIsUnknown() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = stubValueOps(template);
        when(values.get("device-token:unknown")).thenReturn(null);
        DeviceTokenService service = newService(template);

        Optional<String> resolved = service.resolveMemberId("unknown");

        assertThat(resolved).isEmpty();
        verify(template, never()).expire(anyString(), any());
    }
}
