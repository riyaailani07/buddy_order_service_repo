package com.example.orderservice.domain;

/**
 * Lifecycle states of an order.
 */
public enum OrderStatus {
    CREATED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
