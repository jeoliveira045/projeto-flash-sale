package br.com.flashsale.processor.messaging;

import br.com.flashsale.contracts.QueueNames;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfiguration {
    @Bean
    DirectExchange flashSaleExchange() {
        return new DirectExchange(QueueNames.EXCHANGE, true, false);
    }

    @Bean
    Queue orderCreatedQueue() {
        return QueueBuilder.durable(QueueNames.ORDER_CREATED).build();
    }

    @Bean
    Queue orderCompensationQueue() {
        return QueueBuilder.durable(QueueNames.ORDER_COMPENSATION).build();
    }

    @Bean
    Queue orderStatusUpdatedQueue() {
        return QueueBuilder.durable(QueueNames.ORDER_STATUS_UPDATED).build();
    }

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, DirectExchange flashSaleExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(flashSaleExchange).with(QueueNames.ORDER_CREATED);
    }

    @Bean
    Binding orderCompensationBinding(Queue orderCompensationQueue, DirectExchange flashSaleExchange) {
        return BindingBuilder.bind(orderCompensationQueue).to(flashSaleExchange).with(QueueNames.ORDER_COMPENSATION);
    }

    @Bean
    Binding orderStatusUpdatedBinding(Queue orderStatusUpdatedQueue, DirectExchange flashSaleExchange) {
        return BindingBuilder.bind(orderStatusUpdatedQueue).to(flashSaleExchange).with(QueueNames.ORDER_STATUS_UPDATED);
    }

    @Bean
    MessageConverter messageConverter() {
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("br.com.flashsale.contracts");
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}