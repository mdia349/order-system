package com.mdia.platform.inventoryservice.consumer;

import com.mdia.platform.inventoryservice.entity.InventoryItem;
import com.mdia.platform.inventoryservice.entity.ProcessedEvent;
import com.mdia.platform.inventoryservice.repo.InventoryRepository;
import com.mdia.platform.inventoryservice.repo.ProcessedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Component
public class OrderEventsConsumer {

    private final ProcessedEventRepository processedRepo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final InventoryRepository inventoryRepo;
    private final Counter consumed;
    private final Counter deduped;

    public OrderEventsConsumer(ProcessedEventRepository processedRepo,
                               InventoryRepository inventoryRepo,
                               MeterRegistry registry) {
        this.processedRepo = processedRepo;
        this.inventoryRepo = inventoryRepo;
        this.consumed = registry.counter("kafka_events_consumed_total", "service", "inventory-service");
        this.deduped = registry.counter("kafka_events_deduped_total", "service", "inventory-service");
    }

    @KafkaListener(topics = "${app.kafka.ordersTopic}", groupId = "inventory-service")
    @Transactional
    public void onMessage(String payload) throws Exception {
        Map<?, ?> evt = mapper.readValue(payload, Map.class);

        UUID eventId = UUID.fromString((String) evt.get("eventId"));

        if (processedRepo.existsById(eventId)) {
            deduped.increment();
            return;
        }

        consumed.increment();

        String eventType = (String) evt.get("eventType");
        if ("OrderCreated".equals(eventType)) {
            InventoryItem item = inventoryRepo.findById("SKU-CHAIR-1")
                    .orElseThrow(() -> new IllegalStateException("MISSING SKU-CHAIR-1 SEED DATA"));
            item.decrement(1);
        }

        processedRepo.save(new ProcessedEvent(eventId));
    }
}
