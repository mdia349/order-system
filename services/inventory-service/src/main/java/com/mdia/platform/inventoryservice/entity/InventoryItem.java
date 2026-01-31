package com.mdia.platform.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "inventory_item")
public class InventoryItem {

    @Id
    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected InventoryItem() {}

    public InventoryItem(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getSku() {return sku;}
    public int getQuantity() {return quantity;}

    public void decrement(int amount) {
        this.quantity -= amount;
        this.updatedAt = Instant.now();
    }

}
