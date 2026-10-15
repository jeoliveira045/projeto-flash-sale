package br.com.flashsale.checkout.messaging;

import br.com.flashsale.contracts.OrderCreatedEvent;
import br.com.flashsale.contracts.OrderStatusUpdatedEvent;
import br.com.flashsale.contracts.QueueNames;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(OrderCreatedEvent event) {
        publish(QueueNames.ORDER_CREATED, event, event.orderId(), event.correlationId());
    }

    public void publishStatus(OrderStatusUpdatedEvent event) {
        publish(QueueNames.ORDER_STATUS_UPDATED, event, event.orderId() + "-status", event.correlationId());
    }

    private void publish(String routingKey, Object event, String eventId, String correlationId) {
        CorrelationData confirmation = new CorrelationData(eventId);
        rabbitTemplate.convertAndSend(QueueNames.EXCHANGE, routingKey, event, message -> {
            message.getMessageProperties().setCorrelationId(correlationId);
            message.getMessageProperties().setHeader(QueueNames.CORRELATION_ID_HEADER, correlationId);
            return message;
        }, confirmation);
        try {
            CorrelationData.Confirm result = confirmation.getFuture().get(5, TimeUnit.SECONDS);
            if (!result.isAck() || confirmation.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ nao confirmou a publicacao: " + result.getReason());
            }
            log.info("event=message_published eventId={} routingKey={} correlationId={}",
                    eventId, routingKey, correlationId);
        } catch (TimeoutException exception) {
            throw new IllegalStateException("Timeout aguardando confirmacao do RabbitMQ", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Publicacao RabbitMQ interrompida", exception);
        } catch (java.util.concurrent.ExecutionException exception) {
            throw new IllegalStateException("Falha ao publicar no RabbitMQ", exception.getCause());
        }
    }
}