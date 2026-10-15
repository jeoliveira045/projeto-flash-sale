package br.com.flashsale.processor.domain;

import br.com.flashsale.contracts.OrderCompensationEvent;
import br.com.flashsale.contracts.OrderCreatedEvent;
import br.com.flashsale.contracts.OrderStatus;
import br.com.flashsale.contracts.OrderStatusUpdatedEvent;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderProcessor {
    private static final Logger log = LoggerFactory.getLogger(OrderProcessor.class);
    private final ProcessedOrderRepository repository;

    public OrderProcessor(ProcessedOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProcessingResult process(OrderCreatedEvent event) {
        var existingOrder = repository.findById(event.orderId());
        if (existingOrder.isPresent()) {
            ProcessedOrder existing = existingOrder.get();
            if (existing.getStatus() == OrderStatus.CONFIRMED) {
                return new ProcessingResult(new OrderStatusUpdatedEvent(event.orderId(), OrderStatus.CONFIRMED,
                        null, event.correlationId(), existing.getUpdatedAt()), null);
            }
            log.info("event=duplicate_order_replayed orderId={} status={}", event.orderId(), existing.getStatus());
            if (existing.getStatus() == OrderStatus.CANCELLED) {
                return new ProcessingResult(null, new OrderCompensationEvent(event.orderId(), event.productId(),
                        event.quantity(), existing.getFailureReason(), event.correlationId(), existing.getUpdatedAt()));
            }
            return new ProcessingResult(null, null);
        }

        Instant now = Instant.now();
        ProcessedOrder order = new ProcessedOrder(event);
        if (event.simulateFailure()) {
            String reason = event.failureReason() == null || event.failureReason().isBlank()
                    ? "Falha simulada no processamento do pedido" : event.failureReason();
            order.complete(OrderStatus.CANCELLED, reason, now);
            repository.save(order);
            log.warn("event=order_processing_failed orderId={} reason={}", event.orderId(), reason);
                return new ProcessingResult(null, new OrderCompensationEvent(event.orderId(), event.productId(),
                    event.quantity(), reason, event.correlationId(), now));
        }

        order.complete(OrderStatus.CONFIRMED, null, now);
        repository.save(order);
        log.info("event=order_confirmed orderId={} productId={} quantity={}",
                event.orderId(), event.productId(), event.quantity());
        return new ProcessingResult(new OrderStatusUpdatedEvent(event.orderId(), OrderStatus.CONFIRMED,
            null, event.correlationId(), now), null);
        }

        public record ProcessingResult(OrderStatusUpdatedEvent statusEvent,
                       OrderCompensationEvent compensationEvent) {
    }
}