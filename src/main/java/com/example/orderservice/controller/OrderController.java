package com.example.orderservice.controller;

import com.example.orderservice.domain.OrderStatus;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.OrderStatsResponse;
import com.example.orderservice.dto.OrderStatusUpdateRequest;
import com.example.orderservice.dto.PageResponse;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

/**
 * REST endpoints for order CRUD operations.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    /** Whitelist of sortable fields to prevent invalid property errors. */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "orderNumber", "customerName", "customerEmail", "status", "totalAmount", "createdAt", "updatedAt");

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse created = orderService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Paginated, sortable list of orders with optional filters.
     *
     * Example: /api/v1/orders?status=CREATED&page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    public PageResponse<OrderResponse> search(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String customerName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = buildPageable(page, size, sort, direction);
        return orderService.search(status, customerEmail, customerName, pageable);
    }

    @GetMapping("/stats")
    public OrderStatsResponse stats() {
        return orderService.stats();
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @GetMapping("/number/{orderNumber}")
    public OrderResponse findByOrderNumber(@PathVariable String orderNumber) {
        return orderService.findByOrderNumber(orderNumber);
    }

    @PutMapping("/{id}")
    public OrderResponse update(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        return orderService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Builds a safe Pageable, clamping size and validating the sort field.
     */
    private Pageable buildPageable(int page, int size, String sort, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        String sortField = SORTABLE_FIELDS.contains(sort) ? sort : "createdAt";
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(safePage, safeSize, Sort.by(sortDirection, sortField));
    }
}
