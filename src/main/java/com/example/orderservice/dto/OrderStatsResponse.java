package com.example.orderservice.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregate snapshot of all orders in the system.
 *
 * @param totalOrders    number of orders
 * @param totalRevenue   sum of all order totals
 * @param averageOrderValue mean order total (zero when there are no orders)
 * @param countByStatus  number of orders per status
 */
public record OrderStatsResponse(
        long totalOrders,
        BigDecimal totalRevenue,
        BigDecimal averageOrderValue,
        Map<String, Long> countByStatus
) {
}
