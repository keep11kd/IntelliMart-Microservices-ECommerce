package com.intellimart.orderservice.util;

import com.intellimart.orderservice.model.Order;
import com.intellimart.orderservice.model.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class OrderSpecifications {

    private OrderSpecifications() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a Specification to filter orders by status.
     * @param status The order status to filter by.
     * @return A Specification for the given status, or null if status is null.
     */
    public static Specification<Order> withStatus(OrderStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    /**
     * Creates a Specification to filter orders by creation date range.
     * @param startDate The start of the date range (inclusive).
     * @param endDate The end of the date range (inclusive).
     * @return A Specification for the given date range, or null if both dates are null.
     */
    public static Specification<Order> withCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate != null && endDate != null) {
                return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
            } else if (startDate != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            } else if (endDate != null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
            }
            return null; // No date filter applied
        };
    }

    /**
     * Combines multiple specifications with an AND conjunction.
     * @param specs An array of specifications to combine.
     * @return A combined Specification, or null if no specifications are provided or all are null.
     */
    public static Specification<Order> combineAnd(Specification<Order>... specs) {
        Specification<Order> combinedSpec = null;
        for (Specification<Order> spec : specs) {
            if (spec != null) {
                if (combinedSpec == null) {
                    combinedSpec = spec;
                } else {
                    combinedSpec = combinedSpec.and(spec);
                }
            }
        }
        return combinedSpec;
    }
}