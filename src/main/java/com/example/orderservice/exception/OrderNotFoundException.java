package com.example.orderservice.exception;

/**
 * Thrown when an order cannot be found by id or order number.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }

    public static OrderNotFoundException byId(Long id) {
        return new OrderNotFoundException("Order not found with id " + id);
    }

    public static OrderNotFoundException byOrderNumber(String orderNumber) {
        return new OrderNotFoundException("Order not found with orderNumber " + orderNumber);
    }
}
