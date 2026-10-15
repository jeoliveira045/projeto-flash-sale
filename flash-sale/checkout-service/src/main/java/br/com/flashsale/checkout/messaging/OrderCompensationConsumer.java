package br.com.flashsale.checkout.messaging;

import br.com.flashsale.contracts.OrderCompensationEvent;
import br.com.flashsale.contracts.QueueNames;
import br.com.flashsale.checkout.domain.OrderService;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCompensationConsumer {
    private final OrderService orderService;

    public OrderCompensationConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = QueueNames.ORDER_COMPENSATION)
    public void onCompensation(OrderCompensationEvent event) {
        MDC.put("correlationId", event.correlationId());
        try {
            orderService.compensate(event);
        } finally {
            MDC.remove("correlationId");
        }
    }
}