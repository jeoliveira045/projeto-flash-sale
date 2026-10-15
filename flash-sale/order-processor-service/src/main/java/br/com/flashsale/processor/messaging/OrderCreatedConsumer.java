package br.com.flashsale.processor.messaging;

import br.com.flashsale.contracts.OrderCreatedEvent;
import br.com.flashsale.contracts.QueueNames;
import br.com.flashsale.processor.domain.OrderProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);
    private final OrderProcessor orderProcessor;
    private final OrderEventPublisher publisher;

    public OrderCreatedConsumer(OrderProcessor orderProcessor, OrderEventPublisher publisher) {
        this.orderProcessor = orderProcessor;
        this.publisher = publisher;
    }

    @RabbitListener(queues = QueueNames.ORDER_CREATED)
    public void onOrderCreated(OrderCreatedEvent event) {
        MDC.put("correlationId", event.correlationId());
        try {
            log.info("event=order_received orderId={} queue={}", event.orderId(), QueueNames.ORDER_CREATED);
            var result = orderProcessor.process(event);
            if (result.compensationEvent() != null) {
                publisher.publishCompensation(result.compensationEvent());
            }
            if (result.statusEvent() != null) {
                publisher.publishStatus(result.statusEvent());
            }
        } finally {
            MDC.remove("correlationId");
        }
    }
}