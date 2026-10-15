package br.com.flashsale.checkout.api;

import br.com.flashsale.contracts.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record OrderResponse(
        @JsonProperty("pedido_id") String orderId,
        @JsonProperty("produto_id") String productId,
        @JsonProperty("quantidade") int quantity,
        @JsonProperty("cliente_cpf") String customerCpf,
        @JsonProperty("cliente_nome") String customerName,
        @JsonProperty("status") OrderStatus status,
        @JsonProperty("data_hora_atualizacao") Instant updatedAt,
        @JsonProperty("motivo_falha") String failureReason,
        @JsonProperty("correlation_id") String correlationId) {
}