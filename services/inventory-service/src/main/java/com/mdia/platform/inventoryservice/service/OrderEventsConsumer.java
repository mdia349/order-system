package com.mdia.platform.inventoryservice.service;

import com.mdia.platform.inventoryservice.entity.ProcessedEvent;
import com.mdia.platform.inventoryservice.repo.ProcessedEventRepository;
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

    public OrderEventsConsumer(ProcessedEventRepository processedRepo) {
        this.processedRepo = processedRepo;
    }

    @KafkaListener(topics = "${app.kafka.ordersTopic}", groupId = "inventory-service")
    @Transactional
    public void onMessage(String payload) throws Exception {
        Map<?, ?> evt = mapper.readValue(payload, Map.class);

        UUID eventId = UUID.fromString((String) evt.get("eventId"));

        if (processedRepo.existsById(eventId)) {
            return;
        }

        processedRepo.save(new ProcessedEvent(eventId));
    }
}
