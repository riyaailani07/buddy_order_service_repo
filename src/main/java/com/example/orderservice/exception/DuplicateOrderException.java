package com.example.orderservice.exception;

/**
 * Thrown when a request conflicts with the current state of the system.
 */
public class DuplicateOrderException extends RuntimeException {

    public DuplicateOrderException(String message) {
        super(message);
    }
}
