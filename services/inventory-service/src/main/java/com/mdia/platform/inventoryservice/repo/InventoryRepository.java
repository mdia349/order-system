package com.mdia.platform.inventoryservice.repo;

import com.mdia.platform.inventoryservice.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, String> {
}
