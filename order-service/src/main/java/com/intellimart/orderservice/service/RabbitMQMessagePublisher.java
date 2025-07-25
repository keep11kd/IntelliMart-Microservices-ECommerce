package com.intellimart.orderservice.service;

import com.intellimart.orderservice.config.RabbitMQConfig;
import com.intellimart.orderservice.dto.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderPlacedEvent(OrderPlacedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE_NAME,
                RabbitMQConfig.ORDER_PLACED_ROUTING_KEY,
                event
            );
            log.info("Published OrderPlacedEvent for Order ID: {} to exchange '{}' with routing key '{}'",
                     event.getOrderId(), RabbitMQConfig.ORDER_EXCHANGE_NAME, RabbitMQConfig.ORDER_PLACED_ROUTING_KEY);
        } catch (Exception e) {
            log.error("Failed to publish OrderPlacedEvent for Order ID {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
}