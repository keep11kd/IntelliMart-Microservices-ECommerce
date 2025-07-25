package com.intellimart.orderservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.slf4j.Logger; // NEW IMPORT
import org.slf4j.LoggerFactory; // NEW IMPORT

@Configuration
public class RabbitMQConfig {

    // NEW: Explicitly declare a logger for this class
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    public static final String ORDER_EXCHANGE_NAME = "order.exchange";
    public static final String ORDER_PLACED_QUEUE_NAME = "order.placed.queue";
    public static final String ORDER_PLACED_ROUTING_KEY = "order.placed";

    // Bean for Topic Exchange
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE_NAME);
    }

    // Bean for the Queue
    @Bean
    public Queue orderPlacedQueue() {
        return new Queue(ORDER_PLACED_QUEUE_NAME, true); // durable = true
    }

    // Bean for Binding
    @Bean
    public Binding bindingOrderPlaced() {
        return BindingBuilder.bind(orderPlacedQueue())
                             .to(orderExchange())
                             .with(ORDER_PLACED_ROUTING_KEY);
    }

    // NEW: Configure MessageConverter for JSON serialization/deserialization
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // NEW: Configure RabbitTemplate with MessageConverter and Publisher Confirms/Returns
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);

        // Enable mandatory returns for unroutable messages
        rabbitTemplate.setMandatory(true);

        // Configure ConfirmCallback for publisher acknowledgements
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String messageId = (correlationData != null) ? correlationData.getId() : "N/A";
            if (ack) {
                // Message successfully received by RabbitMQ broker
                log.info("Message with ID {} confirmed by RabbitMQ broker.", messageId);
            } else {
                // Message not received by RabbitMQ broker (e.g., connection issue)
                log.error("Message with ID {} NOT confirmed by RabbitMQ broker. Cause: {}", messageId, cause);
                // In a real application, you might implement retry logic or store in a dead-letter table here.
            }
        });

        // Configure ReturnCallback for unroutable messages
        rabbitTemplate.setReturnsCallback(returned -> {
            // Message was returned because it couldn't be routed to any queue
            String message = new String(returned.getMessage().getBody());
            log.warn("Message returned: Exchange: {}, RoutingKey: {}, ReplyCode: {}, ReplyText: {}, Message: {}",
                     returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText(), message);
            // This indicates a configuration error (e.g., wrong routing key, queue not bound).
            // Alerting or specific error handling would be appropriate here.
        });

        return rabbitTemplate;
    }
}