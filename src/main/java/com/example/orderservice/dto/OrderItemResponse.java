package com.example.orderservice.dto;

import java.math.BigDecimal;

/**
 * A single line item returned by the API.
 */
public record OrderItemResponse(
        Long id,
        String productCode,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
