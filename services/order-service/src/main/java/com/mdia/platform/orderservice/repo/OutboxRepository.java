package com.mdia.platform.orderservice.repo;

import com.mdia.platform.orderservice.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = "select * from outbox_event where published_at is null order by occurred_at asc limit 50", nativeQuery = true)
    List<OutboxEvent> findUnpublishedBatch();
}
