package com.intellimart.notificationservice.listener;

import com.intellimart.notificationservice.dto.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException; // NEW IMPORT
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashSet; // NEW IMPORT
import java.util.Set;     // NEW IMPORT

@Component
@Slf4j
public class OrderEventListener {

    public static final String ORDER_PLACED_QUEUE_NAME = "order.placed.queue";

    // NEW: Simple in-memory set to simulate idempotency check for processed order IDs.
    // In a real application, this would be a persistent store (e.g., Redis, database table).
    private final Set<Long> processedOrderIds = new HashSet<>();

    /**
     * Listens for messages on the 'order.placed.queue' and processes OrderPlacedEvent objects.
     * Includes basic idempotency check and enhanced error handling.
     *
     * @param event The deserialized OrderPlacedEvent object received from RabbitMQ.
     */
    @RabbitListener(queues = ORDER_PLACED_QUEUE_NAME)
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Attempting to process Order Placed Event for Order ID: {}", event.getOrderId());

        // --- Graceful Degradation / Idempotency Check ---
        // If this service restarts or a message is redelivered, we don't want to send duplicate notifications.
        // This is a simple in-memory check. For production, use a distributed, persistent store.
        if (processedOrderIds.contains(event.getOrderId())) {
            log.warn("Order ID {} already processed. Skipping duplicate event.", event.getOrderId());
            return; // Acknowledge and discard duplicate
        }

        try {
            // Simulate fetching customer contact details (if not already in event)
            String customerEmail = "customer" + event.getUserId() + "@example.com";
            String customerPhone = "+91-9876543210";

            log.info("--- Initiating Notification Process for Order {} (Order ID: {}) ---", event.getOrderNumber(), event.getOrderId());

            // Simulate sending email notification
            long emailDelayMillis = 2000; // 2 seconds
            log.info("Simulating sending email notification to {}...", customerEmail);
            Thread.sleep(emailDelayMillis);

            String emailSubject = String.format("IntelliMart Order #%s Confirmed!", event.getOrderNumber());
            StringBuilder emailBody = new StringBuilder(String.format(
                "Dear Customer,\n\n" +
                "Your order #%s has been successfully placed and confirmed!\n\n" +
                "Order ID: %d\n" +
                "Total Amount: %.2f INR\n" +
                "Status: %s\n" +
                "Shipping Address: %s\n\n" +
                "Items:\n",
                event.getOrderNumber(),
                event.getOrderId(),
                event.getTotalAmount(),
                event.getStatus(),
                event.getShippingAddress()
            ));

            for (int i = 0; i < event.getItems().size(); i++) {
                emailBody.append(String.format("  %d. %s (Qty: %d, Price: %.2f)\n",
                    i + 1,
                    event.getItems().get(i).getProductName(),
                    event.getItems().get(i).getQuantity(),
                    event.getItems().get(i).getPriceAtPurchase()
                ));
            }
            emailBody.append("\nThank you for your purchase!\nIntelliMart Team");

            log.info("Simulated Email Sent to {}:\nSubject: {}\nBody:\n{}\n", customerEmail, emailSubject, emailBody.toString());

            // Simulate SMS notification
            long smsDelayMillis = 1000; // 1 second
            log.info("Simulating sending SMS notification to {}...", customerPhone);
            Thread.sleep(smsDelayMillis);

            String smsContent = String.format("IntelliMart: Your order #%s for %.2f INR is confirmed. Status: %s. Track at [link].",
                event.getOrderNumber(), event.getTotalAmount(), event.getStatus());

            log.info("Simulated SMS Sent to {}:\nContent: {}\n", customerPhone, smsContent);

            log.info("--- Notification Process Completed Successfully for Order {} ---", event.getOrderNumber());
            processedOrderIds.add(event.getOrderId()); // Mark as processed after successful completion

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            log.error("Notification process interrupted for Order ID {}. Message will be re-queued. Error: {}", event.getOrderId(), e.getMessage());
            // Re-throw to indicate failure and allow RabbitMQ to re-queue
            throw new RuntimeException("Notification processing interrupted", e);
        } catch (Exception e) {
            log.error("Failed to process notification for Order ID {}. Message will be re-queued. Error: {}", event.getOrderId(), e.getMessage(), e);
            // Re-throw to indicate failure and allow RabbitMQ to re-queue
            // For unrecoverable errors, use AmqpRejectAndDontRequeueException
            // throw new AmqpRejectAndDontRequeueException("Unrecoverable error processing event", e);
            throw new RuntimeException("Failed to process notification", e);
        }
        log.info("--------------------------------------------------");
    }
}