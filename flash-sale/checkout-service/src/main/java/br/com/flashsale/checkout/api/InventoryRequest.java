package br.com.flashsale.checkout.api;

import jakarta.validation.constraints.PositiveOrZero;

public record InventoryRequest(@PositiveOrZero int quantity) {
}