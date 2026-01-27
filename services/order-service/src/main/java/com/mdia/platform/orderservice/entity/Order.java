package com.mdia.platform.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Order() {}

    public Order(UUID id, UUID userId, String status) {
        this.id = id;
        this.userId = userId;
        this.status = status;
    }

    public UUID getId() {return id;}
    public UUID getUserId() {return userId;}
    public String getStatus() {return status;}

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
}
