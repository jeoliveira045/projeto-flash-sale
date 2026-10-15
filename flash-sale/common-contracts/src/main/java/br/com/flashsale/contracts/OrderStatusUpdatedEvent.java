package br.com.flashsale.contracts;

import java.time.Instant;

public record OrderStatusUpdatedEvent(
        String orderId,
        OrderStatus status,
        String failureReason,
        String correlationId,
        Instant updatedAt) {
}