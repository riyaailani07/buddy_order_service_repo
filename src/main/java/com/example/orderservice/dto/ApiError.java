package com.example.orderservice.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error body returned for failed requests.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
