package com.example.orderservice.exception;

/**
 * Thrown when an order state transition is not allowed.
 */
public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
