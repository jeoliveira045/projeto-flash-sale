package br.com.flashsale.processor.domain;

import br.com.flashsale.contracts.OrderCreatedEvent;
import br.com.flashsale.contracts.OrderStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "processed_orders")
public class ProcessedOrder {
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

    protected ProcessedOrder() {
    }

    public ProcessedOrder(OrderCreatedEvent event) {
        this.id = event.orderId();
        this.productId = event.productId();
        this.quantity = event.quantity();
        this.customerCpf = event.customerCpf();
        this.customerName = event.customerName();
        this.status = OrderStatus.PROCESSING;
        this.createdAt = event.createdAt();
        this.updatedAt = event.createdAt();
        this.correlationId = event.correlationId();
    }

    public void complete(OrderStatus newStatus, String reason, Instant changedAt) {
        this.status = newStatus;
        this.failureReason = reason;
        this.updatedAt = changedAt;
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