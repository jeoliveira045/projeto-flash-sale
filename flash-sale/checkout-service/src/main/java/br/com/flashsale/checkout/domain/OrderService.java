package br.com.flashsale.checkout.domain;

import br.com.flashsale.contracts.OrderCompensationEvent;
import br.com.flashsale.contracts.OrderCreatedEvent;
import br.com.flashsale.contracts.OrderStatus;
import br.com.flashsale.contracts.OrderStatusUpdatedEvent;
import br.com.flashsale.contracts.QueueNames;
import br.com.flashsale.checkout.api.CreateOrderRequest;
import br.com.flashsale.checkout.api.OrderResponse;
import br.com.flashsale.checkout.messaging.OrderEventPublisher;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final StockService stockService;
    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(StockService stockService, OrderRepository orderRepository,
                        OrderEventPublisher orderEventPublisher) {
        this.stockService = stockService;
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    public OrderResponse create(CreateOrderRequest request, String correlationId) {
        if (!stockService.reserve(request.productId(), request.quantity())) {
            log.warn("event=order_rejected reason=insufficient_stock productId={} quantity={}",
                    request.productId(), request.quantity());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Estoque insuficiente para o produto " + request.productId());
        }

        Instant now = Instant.now();
        String orderId = UUID.randomUUID().toString();
        OrderEntity order = new OrderEntity(orderId, request.productId(), request.quantity(),
                request.customerCpf(), request.customerName(), OrderStatus.PROCESSING, now,
                null, correlationId);
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, request.productId(), request.quantity(),
                request.customerCpf(), request.customerName(), request.simulateFailure(),
                request.failureReason(), correlationId, now);
        try {
            orderRepository.save(order);
            orderEventPublisher.publish(event);
        } catch (RuntimeException exception) {
            stockService.compensate(orderId, request.productId(), request.quantity());
            order.updateStatus(OrderStatus.CANCELLED, "Falha ao publicar pedido no RabbitMQ", Instant.now());
            try {
                orderRepository.save(order);
            } catch (RuntimeException persistenceException) {
                exception.addSuppressed(persistenceException);
            }
            log.error("event=order_publish_failed orderId={} queue={} reason={}",
                    orderId, QueueNames.ORDER_CREATED, exception.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Nao foi possivel publicar o pedido; estoque devolvido", exception);
        }
        return order.toResponse();
    }

    @Cacheable(cacheNames = "orders", key = "#p0")
    public OrderResponse findById(String orderId) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            var order = orderRepository.findById(orderId);
            if (order.isPresent()) {
                return order.get().toResponse();
            }
            log.info("event=order_lookup_miss orderId={} attempt={}/3", orderId, attempt);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado");
    }

    @CacheEvict(cacheNames = "orders", key = "#p0.orderId()")
    public void applyStatus(OrderStatusUpdatedEvent event) {
        OrderEntity order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException("Pedido local inexistente: " + event.orderId()));
        order.updateStatus(event.status(), event.failureReason(), event.updatedAt());
        orderRepository.save(order);
        log.info("event=order_status_updated orderId={} status={}", event.orderId(), event.status());
    }

    public void compensate(OrderCompensationEvent event) {
        boolean changed = stockService.compensate(event.orderId(), event.productId(), event.quantity());
        log.info("event=stock_compensated orderId={} productId={} quantity={} applied={}",
                event.orderId(), event.productId(), event.quantity(), changed);
        OrderStatusUpdatedEvent statusEvent = new OrderStatusUpdatedEvent(event.orderId(),
                OrderStatus.CANCELLED, event.reason(), event.correlationId(), Instant.now());
        orderEventPublisher.publishStatus(statusEvent);
    }
}