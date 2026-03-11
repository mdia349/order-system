package com.mdia.platform.shippingservice.consumer;

import com.mdia.platform.shippingservice.entity.OutboxEvent;
import com.mdia.platform.shippingservice.entity.ProcessedEvent;
import com.mdia.platform.shippingservice.entity.Shipment;
import com.mdia.platform.shippingservice.entity.ShipmentStatus;
import com.mdia.platform.shippingservice.repo.OutboxRepository;
import com.mdia.platform.shippingservice.repo.ProcessedEventRepository;
import com.mdia.platform.shippingservice.repo.ShipmentRepository;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.transaction.Transactional;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderEventsConsumer {

    private final ProcessedEventRepository processedEventRepo;
    private final ShipmentRepository shipmentRepo;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("shipping-service");

    public OrderEventsConsumer(ProcessedEventRepository processedEventRepo,
                               ShipmentRepository shipmentRepo,
                               OutboxRepository outboxRepository) {
        this.processedEventRepo = processedEventRepo;
        this.shipmentRepo = shipmentRepo;
        this.outboxRepository = outboxRepository;
    }

    @KafkaListener(topics = "${app.kafka.ordersTopic}", groupId = "shipping-service")
    @Transactional
    public void onMessage(String payload) throws Exception {
        Span span = tracer.spanBuilder("shipping.consumeOrderEvent").startSpan();
        try (Scope scope = span.makeCurrent()){

            Map<?, ?> evt = mapper.readValue(payload, Map.class);
            UUID eventId = UUID.fromString((String) evt.get("eventId"));
            String eventType = (String) evt.get("eventType");

            span.setAttribute("event.id", eventId.toString());
            span.setAttribute("event.type", eventType);

            if (processedEventRepo.existsById(eventId)) {
                span.setAttribute("event.deduped", true);
                return;
            }

            if ("InventoryReserved".equals(eventType)) {
                UUID orderId = UUID.fromString((String) evt.get("aggregateId"));

                Shipment shipment = shipmentRepo.findByOrderId(orderId).orElseGet(() ->
                        shipmentRepo.save(new Shipment(UUID.randomUUID(), orderId, ShipmentStatus.CREATED))
                );

                String shipmentPayload = mapper.writeValueAsString(Map.of(
                        "eventId", UUID.randomUUID().toString(),
                        "eventType", "ShipmentCreated",
                        "aggregateType", "Shipment",
                        "aggregateId", shipment.getId().toString(),
                        "data", Map.of(
                                "orderId", orderId.toString(),
                                "status", shipment.getStatus().name()
                        )
                ));

                outboxRepository.save(new OutboxEvent(
                        UUID.randomUUID(),
                        "Shipment",
                        shipment.getId(),
                        "ShipmentCreated",
                        shipmentPayload,
                        Instant.now()
                ));
            }

            processedEventRepo.save(new ProcessedEvent(eventId));
        } finally {
            span.end();
        }
    }
}
