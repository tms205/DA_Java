package com.huit.da_java.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderNotificationServiceTest {

    @Test
    void subscribeReturnsEmitterAndTracksSubscriber() {
        OrderNotificationService service = new OrderNotificationService();

        SseEmitter emitter = service.subscribe();

        assertNotNull(emitter);
        assertEquals(1, service.getSubscriberCount());
    }

    @Test
    void notifyNewOrderDoesNotCrashWhenEmitterFails() {
        OrderNotificationService service = new OrderNotificationService() {
            @Override
            protected SseEmitter createEmitter(long timeoutMs) {
                return new FailingEmitter(timeoutMs);
            }
        };
        service.subscribe();

        OrderNotification notification = new OrderNotification(
                10,
                "Test Customer",
                "50,000 VND",
                "No ice",
                LocalDateTime.now());

        assertDoesNotThrow(() -> service.notifyNewOrder(notification));
        assertEquals(0, service.getSubscriberCount());
    }

    private static class FailingEmitter extends SseEmitter {
        FailingEmitter(long timeoutMs) {
            super(timeoutMs);
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            throw new IOException("stream closed");
        }
    }
}
