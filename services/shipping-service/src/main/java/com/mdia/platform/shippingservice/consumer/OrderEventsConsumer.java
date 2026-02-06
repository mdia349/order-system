package com.mdia.platform.shippingservice.consumer;

import com.mdia.platform.shippingservice.entity.ProcessedEvent;
import com.mdia.platform.shippingservice.entity.Shipment;
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

import java.util.Map;
import java.util.UUID;

@Component
public class OrderEventsConsumer {

    private final ProcessedEventRepository processedEventRepo;
    private final ShipmentRepository shipmentRepo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("shipping-service");

    public OrderEventsConsumer(ProcessedEventRepository processedEventRepo, ShipmentRepository shipmentRepo) {
        this.processedEventRepo = processedEventRepo;
        this.shipmentRepo = shipmentRepo;
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

            if ("OrderCreated".equals(eventType)) {
                UUID orderId = UUID.fromString((String) evt.get("aggregateId"));

                shipmentRepo.findByOrderId(orderId).orElseGet(() ->
                        shipmentRepo.save(new Shipment(UUID.randomUUID(), orderId, "CREATED"))
                );
            }

            processedEventRepo.save(new ProcessedEvent(eventId));
        } finally {
            span.end();
        }
    }
}
