package com.intellimart.orderservice.service;

import com.intellimart.orderservice.config.RabbitMQConfig;
import com.intellimart.orderservice.dto.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData; // NEW IMPORT
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID; // NEW IMPORT

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes an OrderPlacedEvent to the RabbitMQ exchange.
     * Includes a CorrelationData for publisher confirms.
     *
     * @param event The OrderPlacedEvent object to be published.
     */
    public void publishOrderPlacedEvent(OrderPlacedEvent event) {
        // Generate a unique ID for this message for tracking confirms/returns
        String messageId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(messageId);

        try {
            log.info("Attempting to publish OrderPlacedEvent for Order ID: {} with Message ID: {}", event.getOrderId(), messageId);
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE_NAME,
                RabbitMQConfig.ORDER_PLACED_ROUTING_KEY,
                event,
                correlationData // Pass CorrelationData for tracking
            );
            // The actual confirmation will be handled by the RabbitTemplate's ConfirmCallback
            // This line only indicates the message was sent *to* the broker, not necessarily confirmed *by* it yet.
        } catch (Exception e) {
            log.error("Failed to send OrderPlacedEvent for Order ID {} to RabbitMQ. Message ID: {}. Error: {}",
                     event.getOrderId(), messageId, e.getMessage(), e);
            // This catch block handles immediate connection/serialization issues.
            // For broker-side failures (routing, queue issues), ConfirmCallback/ReturnCallback will fire.
            // Consider a fallback mechanism here (e.g., storing the event in a database for later retry).
        }
    }
}