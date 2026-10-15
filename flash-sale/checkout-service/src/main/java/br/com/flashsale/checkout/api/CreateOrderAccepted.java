package br.com.flashsale.checkout.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record CreateOrderAccepted(
        @JsonProperty("pedido_id") String orderId,
        @JsonProperty("status") String status,
        @JsonProperty("data_hora_requisicao") Instant requestedAt,
        @JsonProperty("correlation_id") String correlationId) {
}