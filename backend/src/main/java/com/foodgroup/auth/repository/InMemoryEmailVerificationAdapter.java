package com.foodgroup.auth.repository;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryEmailVerificationAdapter implements EmailVerificationPort {

    private static final long TTL_SECONDS = 300;

    private record Entry(String code, Instant expiresAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void saveCode(String email, String code) {
        store.put(email, new Entry(code, Instant.now().plusSeconds(TTL_SECONDS)));
    }

    @Override
    public Optional<String> findCode(String email) {
        Entry entry = store.get(email);
        if (entry == null) return Optional.empty();
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(email);
            return Optional.empty();
        }
        return Optional.of(entry.code());
    }

    @Override
    public void deleteCode(String email) {
        store.remove(email);
    }
}
