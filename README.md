#  Event-Driven Order Management System

---

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

---

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

---

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

#  Event Flow (Current)

---

1. Client places order
2. Order Service:
   - Persists order
   - Writes outbox event
3. Kafka publishes `OrderCreated`
4. Inventory Service:
   - Reserves stock
   - Emits `InventoryReserved`
5. Shipping Service:
   - Creates shipment
   - Emits `ShipmentCreated`

All services participate in a shared distributed trace visible in Jaeger.

---

#  Observability

---

## Distributed Tracing

- OpenTelemetry SDK in each service
- Context propagation across Kafka
- Traces visualized in Jaeger
- Producer + consumer spans in same trace

## Metrics Pipeline

Services → OTel Collector → Prometheus → Grafana

Examples:
- HTTP request metrics
- JVM memory usage
- Custom business metrics (in progress)

---

#  Running the Project

---

```bash
docker compose up --build
```

### Access:

- **Jaeger:** http://localhost:16686
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000

---

# Tech Stack

---

- Java 21
- Spring Boot
- Kafka
- PostgreSQL
- OpenTelemetry
- Jaeger
- Prometheus
- Grafana
- Docker

---

# Road Map

---

- [ ] Saga choreography with order state transitions
- [ ] Dead letter queue + retry
- [ ] Integration testing with Testcontainers
- [ ] Kubernetes deployment

