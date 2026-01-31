package com.mdia.platform.inventoryservice;

import com.mdia.platform.inventoryservice.entity.InventoryItem;
import com.mdia.platform.inventoryservice.repo.InventoryRepository;
import com.mdia.platform.inventoryservice.repo.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
public class InventoryConsumerIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16")
                    .withDatabaseName("inventorydb")
                    .withUsername("inventory")
                    .withPassword("inventorypass");

    @Container
    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("apache/kafka:latest"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("app.kafka.ordersTopic", () -> "orders.events");
    }

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    InventoryRepository inventoryRepo;
    @Autowired
    ProcessedEventRepository processedEventRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        processedEventRepo.deleteAll();
        inventoryRepo.deleteAll();
        inventoryRepo.save(new InventoryItem("SKU-CHAIR-1", 10));
    }

    @Test
    void processesEvent_writesProcessedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        String payload = payload(eventId);

        kafkaTemplate.send("orders.events", "k1", payload);

        waitUntil(() -> processedEventRepo.existsById(eventId));
        assertThat(processedEventRepo.count()).isEqualTo(1);
    }

    @Test
    void processesEvent_decrementInventory() throws Exception {
        UUID eventId = UUID.randomUUID();
        String payload = payload(eventId);

        kafkaTemplate.send("orders.events", "k1", payload);

        waitUntil(() -> processedEventRepo.existsById(eventId));

        int qty = inventoryRepo.findById("SKU-CHAIR-1").orElseThrow().getQuantity();
        assertThat(qty).isEqualTo(9);
    }

    @Test
    void duplicateEventId_isDeduped_inventoryDecrementsOnce() throws Exception {
        UUID eventId = UUID.randomUUID();
        String payload = payload(eventId);

        kafkaTemplate.send("orders.events", "k1", payload);
        kafkaTemplate.send("orders.events", "k1", payload);

        waitUntil(() -> processedEventRepo.existsById(eventId));

        assertThat(processedEventRepo.count()).isEqualTo(1);

        int qty = inventoryRepo.findById("SKU-CHAIR-1").orElseThrow().getQuantity();
        assertThat(qty).isEqualTo(9);
    }

    private static void waitUntil(Check condition) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (condition.ok()) return;
            } catch (Exception ignored) {}
        }
        throw new AssertionError("Condition not met within timeout");
    }

    @FunctionalInterface
    interface Check {
        boolean ok() throws Exception;
    }

    private String payload(UUID eventId) throws Exception {
        UUID orderId = UUID.randomUUID();
        return mapper.writeValueAsString(Map.of(
                "eventId", eventId.toString(),
                "eventType", "OrderCreated",
                "aggregateType", "Order",
                "aggregateId", orderId.toString(),
                "occurredAt", Instant.now().toString(),
                "data", Map.of("userId", UUID.randomUUID().toString())
        ));
    }
}
