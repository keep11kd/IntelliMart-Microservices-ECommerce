package com.intellimart.notificationservice.model;

public enum OrderStatus {
	  PENDING,       // Order received, awaiting processing
	    PROCESSING,    // Order is being prepared/fulfilled
	    CONFIRMED,     // Order is confirmed and ready for shipping/pickup
	    SHIPPED,       // Order has been shipped (for physical goods)
	    DELIVERED,     // Order has been delivered
	    CANCELLED,     // Order was cancelled
	    REFUNDED,      // Order was refunded
	    FAILED,         // Order processing failed
	    PENDING_PAYMENT, // <--- ADDED
	    AUTHORIZED,      // <--- ADDED
	    PAID,            // <--- ADDED
}
