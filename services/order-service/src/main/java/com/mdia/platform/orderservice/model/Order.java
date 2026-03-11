package com.mdia.platform.orderservice.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Order() {}

    public Order(UUID id, UUID userId, OrderStatus status) {
        this.id = id;
        this.userId = userId;
        this.status = status;
    }

    public UUID getId() {return id;}
    public UUID getUserId() {return userId;}
    public OrderStatus getStatus() {return status;}

//    public void setStatus(OrderStatus status) {
//        this.status = status;
//        this.updatedAt = Instant.now();
//    }

    public void markInventoryReserved() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("Invalid transition");
        }
        this.status = OrderStatus.INVENTORY_RESERVED;
    }

    public void markCompleted() {
        if (this.status != OrderStatus.INVENTORY_RESERVED) {
            throw new IllegalStateException("Invalid transition");
        }
        this.status = OrderStatus.COMPLETED;
    }

}
