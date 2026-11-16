package com.example.orderservice.dto;

import com.example.orderservice.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for changing only the status of an order.
 */
public record OrderStatusUpdateRequest(

        @NotNull(message = "status is required")
        OrderStatus status
) {
}
