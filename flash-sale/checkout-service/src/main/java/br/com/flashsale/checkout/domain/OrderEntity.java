package br.com.flashsale.checkout.domain;

import br.com.flashsale.contracts.OrderStatus;
import br.com.flashsale.checkout.api.OrderResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "checkout_orders")
public class OrderEntity {
    @Id
    private String id;
    private String productId;
    private int quantity;
    private String customerCpf;
    private String customerName;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String failureReason;
    private String correlationId;

    protected OrderEntity() {
    }

    public OrderEntity(String id, String productId, int quantity, String customerCpf,
                       String customerName, OrderStatus status, Instant createdAt,
                       String failureReason, String correlationId) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.customerCpf = customerCpf;
        this.customerName = customerName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.failureReason = failureReason;
        this.correlationId = correlationId;
    }

    public void updateStatus(OrderStatus newStatus, String reason, Instant changedAt) {
        this.status = newStatus;
        this.failureReason = reason;
        this.updatedAt = changedAt;
    }

    public OrderResponse toResponse() {
        return new OrderResponse(id, productId, quantity, customerCpf, customerName,
                status, updatedAt, failureReason, correlationId);
    }

    public String getId() { return id; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public String getCustomerCpf() { return customerCpf; }
    public String getCustomerName() { return customerName; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getFailureReason() { return failureReason; }
    public String getCorrelationId() { return correlationId; }
}