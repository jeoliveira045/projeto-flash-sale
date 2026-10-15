package br.com.flashsale.checkout.messaging;

import br.com.flashsale.contracts.OrderStatusUpdatedEvent;
import br.com.flashsale.contracts.QueueNames;
import br.com.flashsale.checkout.domain.OrderService;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusConsumer {
    private final OrderService orderService;

    public OrderStatusConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = QueueNames.ORDER_STATUS_UPDATED)
    public void onStatusUpdated(OrderStatusUpdatedEvent event) {
        MDC.put("correlationId", event.correlationId());
        try {
            orderService.applyStatus(event);
        } finally {
            MDC.remove("correlationId");
        }
    }
}