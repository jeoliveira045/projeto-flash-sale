package br.com.flashsale.checkout.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @JsonProperty("produto_id") @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,80}") String productId,
        @JsonProperty("quantidade") @Positive int quantity,
        @JsonProperty("cliente_cpf") @NotBlank @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 digitos") String customerCpf,
        @JsonProperty("cliente_nome") @NotBlank String customerName,
        @JsonProperty("simular_falha") boolean simulateFailure,
        @JsonProperty("motivo_falha") String failureReason) {
}