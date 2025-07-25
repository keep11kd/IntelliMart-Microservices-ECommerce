package com.intellimart.orderservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE_NAME = "order.exchange";
    public static final String ORDER_PLACED_QUEUE_NAME = "order.placed.queue";
    public static final String ORDER_PLACED_ROUTING_KEY = "order.placed";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE_NAME);
    }

    @Bean
    public Queue orderPlacedQueue() {
        return new Queue(ORDER_PLACED_QUEUE_NAME, true);
    }

    @Bean
    public Binding bindingOrderPlaced() {
        return BindingBuilder.bind(orderPlacedQueue())
                             .to(orderExchange())
                             .with(ORDER_PLACED_ROUTING_KEY);
    }
}