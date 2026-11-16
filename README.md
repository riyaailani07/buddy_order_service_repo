# Order Service

A sample **CRUD order service** built with **Java 21** and **Spring Boot 3.3**.

It exposes a REST API for creating, reading, updating and deleting customer orders,
with line items, automatic total calculation, and a validated order lifecycle.

---

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.4 |
| Persistence | Spring Data JPA + Hibernate |
| Database | H2 in-memory (swap for Postgres/MySQL easily) |
| Validation | Jakarta Bean Validation |
| Build | Maven |
| Tests | JUnit 5 + MockMvc |

---

## Getting Started

### Run locally

```bash
mvn spring-boot:run
```

The service starts on **http://localhost:8080**.

### Run tests

```bash
mvn test
```

### Run with Docker

```bash
docker build -t order-service .
docker run -p 8080:8080 order-service
```

### H2 console

Browse **http://localhost:8080/h2-console** with:

- JDBC URL: `jdbc:h2:mem:ordersdb`
- User: `sa`
- Password: *(empty)*

---

## API Reference

Base path: `/api/v1/orders`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/orders` | Create an order |
| `GET` | `/api/v1/orders` | List orders (optional `?status=` or `?customerEmail=`) |
| `GET` | `/api/v1/orders/{id}` | Get an order by id |
| `GET` | `/api/v1/orders/number/{orderNumber}` | Get an order by order number |
| `PUT` | `/api/v1/orders/{id}` | Replace an order's details and items |
| `PATCH` | `/api/v1/orders/{id}/status` | Change only the order status |
| `DELETE` | `/api/v1/orders/{id}` | Delete an order |

### Create an order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Riya Ailani",
    "customerEmail": "riya@example.com",
    "items": [
      {"productCode": "SKU-1", "productName": "Mechanical Keyboard", "quantity": 2, "unitPrice": 2499.00},
      {"productCode": "SKU-2", "productName": "Mouse Pad", "quantity": 1, "unitPrice": 499.00}
    ]
  }'
```

Response `201 Created`:

```json
{
  "id": 1,
  "orderNumber": "ORD-2026-8F3A1C2D",
  "customerName": "Riya Ailani",
  "customerEmail": "riya@example.com",
  "status": "CREATED",
  "totalAmount": 5497.00,
  "items": [
    {"id": 1, "productCode": "SKU-1", "productName": "Mechanical Keyboard", "quantity": 2, "unitPrice": 2499.00, "lineTotal": 4998.00},
    {"id": 2, "productCode": "SKU-2", "productName": "Mouse Pad", "quantity": 1, "unitPrice": 499.00, "lineTotal": 499.00}
  ],
  "createdAt": "2026-11-16T13:55:00Z",
  "updatedAt": "2026-11-16T13:55:00Z"
}
```

### List and filter

```bash
curl "http://localhost:8080/api/v1/orders"
curl "http://localhost:8080/api/v1/orders?status=CONFIRMED"
curl "http://localhost:8080/api/v1/orders?customerEmail=riya@example.com"
```

### Update status

```bash
curl -X PATCH http://localhost:8080/api/v1/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "CONFIRMED"}'
```

### Delete

```bash
curl -X DELETE http://localhost:8080/api/v1/orders/1
```

---

## Order Lifecycle

```
CREATED ──► CONFIRMED ──► SHIPPED ──► DELIVERED
   │            │
   └────────────┴──► CANCELLED
```

Invalid transitions return **422 Unprocessable Entity**. Orders in `DELIVERED` or
`CANCELLED` can no longer be modified.

---

## Error Format

All errors share one shape:

```json
{
  "timestamp": "2026-11-16T13:55:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/orders",
  "fieldErrors": {
    "customerEmail": "customerEmail must be a valid email address"
  }
}
```

| Status | When |
|---|---|
| `400` | Validation failure |
| `404` | Order not found |
| `409` | Conflict |
| `422` | Invalid state transition |
| `500` | Unexpected error |

---

## Project Layout

```
src/main/java/com/example/orderservice/
├── OrderServiceApplication.java
├── controller/OrderController.java
├── domain/          Order, OrderItem, OrderStatus
├── dto/             Request/response records + ApiError
├── exception/       Custom exceptions + GlobalExceptionHandler
├── repository/      OrderRepository
└── service/         OrderService
```

---

## Possible Next Steps

- Replace H2 with PostgreSQL and add Flyway migrations
- Add pagination to the list endpoint
- Add Spring Security with JWT
- Publish OpenAPI docs via springdoc
- Add Testcontainers for real-database integration tests
