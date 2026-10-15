package br.com.flashsale.contracts;

import java.time.Instant;

public record OrderCreatedEvent(
        String orderId,
        String productId,
        int quantity,
        String customerCpf,
        String customerName,
        boolean simulateFailure,
        String failureReason,
        String correlationId,
        Instant createdAt) {
}