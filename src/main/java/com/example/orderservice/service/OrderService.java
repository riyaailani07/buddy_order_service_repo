package com.example.orderservice.service;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderItem;
import com.example.orderservice.domain.OrderStatus;
import com.example.orderservice.dto.OrderItemRequest;
import com.example.orderservice.dto.OrderItemResponse;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.OrderStatsResponse;
import com.example.orderservice.dto.PageResponse;
import com.example.orderservice.exception.InvalidOrderStateException;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic for creating, reading, updating and deleting orders.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        applyRequest(order, request);
        order.setStatus(request.status() == null ? OrderStatus.CREATED : request.status());
        order.recalculateTotal();
        return toResponse(orderRepository.save(order));
    }

    /**
     * Paginated search. Filters are optional and combine as follows:
     * status takes precedence, then customerEmail, then a name search.
     */
    public PageResponse<OrderResponse> search(OrderStatus status,
                                              String customerEmail,
                                              String customerName,
                                              Pageable pageable) {
        Page<Order> page;
        if (status != null) {
            page = orderRepository.findByStatus(status, pageable);
        } else if (customerEmail != null && !customerEmail.isBlank()) {
            page = orderRepository.findByCustomerEmailIgnoreCase(customerEmail.trim(), pageable);
        } else if (customerName != null && !customerName.isBlank()) {
            page = orderRepository.findByCustomerNameContainingIgnoreCase(customerName.trim(), pageable);
        } else {
            page = orderRepository.findAll(pageable);
        }
        return PageResponse.from(page, OrderService::toResponse);
    }

    public OrderResponse findById(Long id) {
        return toResponse(getOrder(id));
    }

    public OrderResponse findByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> OrderNotFoundException.byOrderNumber(orderNumber));
        return toResponse(order);
    }

    /**
     * Aggregate metrics across every order.
     */
    public OrderStatsResponse stats() {
        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.sumTotalAmount();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        BigDecimal average = totalOrders == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);

        Map<String, Long> countByStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            countByStatus.put(status.name(), orderRepository.countByStatus(status));
        }

        return new OrderStatsResponse(totalOrders, totalRevenue, average, countByStatus);
    }

    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        Order order = getOrder(id);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException(
                    "Order " + order.getOrderNumber() + " is " + order.getStatus() + " and can no longer be modified");
        }
        applyRequest(order, request);
        if (request.status() != null) {
            order.setStatus(request.status());
        }
        order.recalculateTotal();
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = getOrder(id);
        validateTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public void delete(Long id) {
        Order order = getOrder(id);
        orderRepository.delete(order);
    }

    private Order getOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> OrderNotFoundException.byId(id));
    }

    private void applyRequest(Order order, OrderRequest request) {
        order.setCustomerName(request.customerName().trim());
        order.setCustomerEmail(request.customerEmail().trim().toLowerCase());

        List<OrderItem> items = request.items().stream()
                .map(this::toEntity)
                .toList();
        order.replaceItems(items);
    }

    private OrderItem toEntity(OrderItemRequest itemRequest) {
        OrderItem item = new OrderItem();
        item.setProductCode(itemRequest.productCode().trim());
        item.setProductName(itemRequest.productName().trim());
        item.setQuantity(itemRequest.quantity());
        item.setUnitPrice(itemRequest.unitPrice());
        return item;
    }

    /**
     * Enforces a simple forward-only lifecycle.
     */
    private void validateTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return;
        }
        boolean allowed = switch (current) {
            case CREATED -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new InvalidOrderStateException(
                    "Cannot move order from " + current + " to " + next);
        }
    }

    private String generateOrderNumber() {
        String candidate;
        do {
            candidate = "ORD-" + Year.now().getValue() + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (orderRepository.existsByOrderNumber(candidate));
        return candidate;
    }

    private static OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductCode(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.lineTotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
