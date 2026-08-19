package com.foodgroup.chat.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

// Spring doesn't expose "how many STOMP sessions are currently open" anywhere on its own;
// this tracks it from the session lifecycle events so it shows up as a normal gauge in
// /actuator/metrics and the Spring Boot Admin dashboard.
@Component
public class WebSocketConnectionMetrics {

    private final AtomicInteger activeSessions = new AtomicInteger(0);

    public WebSocketConnectionMetrics(MeterRegistry registry) {
        registry.gauge("chat.websocket.active.sessions", activeSessions);
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        activeSessions.incrementAndGet();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        activeSessions.updateAndGet(current -> Math.max(0, current - 1));
    }
}
