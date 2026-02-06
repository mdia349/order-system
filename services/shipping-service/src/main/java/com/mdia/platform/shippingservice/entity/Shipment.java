package com.mdia.platform.shippingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipment")
public class Shipment {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Shipment() {}

    public Shipment(UUID id, UUID orderId, String status) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
    }

    public UUID getId() {return id;}
    public UUID getOrderId() {return orderId;}
    public String getStatus() {return status;}
}
