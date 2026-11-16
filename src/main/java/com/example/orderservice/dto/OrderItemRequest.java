package com.example.orderservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * A single line item in an order request.
 */
public record OrderItemRequest(

        @NotBlank(message = "productCode is required")
        @Size(max = 40, message = "productCode must be at most 40 characters")
        String productCode,

        @NotBlank(message = "productName is required")
        @Size(max = 160, message = "productName must be at most 160 characters")
        String productName,

        @Min(value = 1, message = "quantity must be at least 1")
        int quantity,

        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.01", message = "unitPrice must be greater than zero")
        BigDecimal unitPrice
) {
}
