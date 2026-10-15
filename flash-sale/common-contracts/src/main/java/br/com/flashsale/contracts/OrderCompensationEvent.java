package br.com.flashsale.contracts;

import java.time.Instant;

public record OrderCompensationEvent(
        String orderId,
        String productId,
        int quantity,
        String reason,
        String correlationId,
        Instant occurredAt) {
}