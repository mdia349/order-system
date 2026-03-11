#  Event-Driven Order Management System

A production-style, event-driven microservices system built with Spring Boot, Kafka, PostgreSQL, and OpenTelemetry.

**This project includes:**

- Distributed tracing
- Event-driven communication
- Transactional Outbox pattern
- Idempotent consumers
- Observability with OpenTelemetry
- Prometheus + Grafana metrics
- Saga choreography (in progress)

---

#  Architecture Overview

```
Client
    |
    ▼
Auth Service (JWT)
    |
    ▼
Order Service ──► Kafka ──► Inventory Service ──► Kafka ──► Shipping Service
|                                                                          | 
└────────────────────── Observability via OpenTelemetry ───────────────────┘ 
```


### Infrastructure Components

- Kafka
- PostgreSQL
- OpenTelemetry Collector
- Jaeger
- Prometheus
- Grafana
- Docker Compose (local orchestration)

---

#  Services

##  Auth Service
- Issues JWT tokens
- Secures service endpoints
- PostgreSQL-backed user storage

##  Order Service
- Creates orders
- Uses **Transactional Outbox Pattern**
- Publishes `OrderCreated` event
- Emits distributed traces

##  Inventory Service
- Consumes `OrderCreated`
- Implements **idempotent consumer logic**
- Reserves inventory
- Publishes `InventoryReserved`

##  Shipping Service
- Consumes inventory events
- Creates shipment records
- Publishes `ShipmentCreated`
- Exposes Micrometer metrics via OpenTelemetry

---

#  Event Flow (Saga Choreography)

1. **Order Service**: Client places order. Persists order (CREATED) and `OrderCreated` outbox event.
2. **Inventory Service**: Consumes `OrderCreated`. Reserves stock, persists `InventoryReserved` outbox event.
3. **Order Service**: Consumes `InventoryReserved`. Updates order status to `INVENTORY_RESERVED`.
4. **Shipping Service**: Consumes `OrderCreated` (Planned: `InventoryReserved`). Creates shipment, persists `ShipmentCreated` outbox event.
5. **Order Service**: Consumes `ShipmentCreated`. Updates order status to `COMPLETED`.

---

#  Observability

## Distributed Tracing

- OpenTelemetry SDK in each service
- Context propagation across Kafka
- Traces visualized in Jaeger
- Producer + consumer spans in same trace (Manual span creation in consumers)

## Metrics Pipeline

Services → OTel Collector → Prometheus → Grafana

Examples:
- HTTP request metrics
- JVM memory usage
- Custom business metrics: `kafka_events_consumed_total`, `kafka_events_deduped_total` (Inventory Service)

---

#  Running the Project

```bash
docker compose up --build
```

### Access:

- **Jaeger:** http://localhost:16686
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000

---

# Tech Stack

- Java 17 (Services) / Java 21 (Docker)
- Spring Boot 3.3.5 / 4.0.2
- Kafka
- PostgreSQL
- OpenTelemetry
- Jaeger
- Prometheus
- Grafana
- Docker

---

# Road Map

- [x] Basic Event-driven communication
- [x] Transactional Outbox pattern
- [x] Distributed tracing with OpenTelemetry
- [ ] Saga choreography refinement (Fixing event dependencies)
- [ ] Dead letter queue + retry
- [ ] Integration testing with Testcontainers
- [ ] Kubernetes deployment

