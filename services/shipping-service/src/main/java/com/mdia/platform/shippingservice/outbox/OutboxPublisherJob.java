package com.mdia.platform.shippingservice.outbox;

import com.mdia.platform.shippingservice.entity.OutboxEvent;
import com.mdia.platform.shippingservice.kafka.OutboxKafkaPublisher;
import com.mdia.platform.shippingservice.repo.OutboxRepository;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisherJob {

    private final OutboxRepository outboxRepository;
    private final OutboxKafkaPublisher outboxKafkaPublisher;
    private final String shipmentsTopic;
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("shipping-service");

    public OutboxPublisherJob(
            OutboxRepository outboxRepository,
            OutboxKafkaPublisher outboxKafkaPublisher,
            @Value("${app.kafka.shipmentsTopic}") String shipmentsTopic
    ) {
        this.outboxRepository = outboxRepository;
        this.outboxKafkaPublisher = outboxKafkaPublisher;
        this.shipmentsTopic = shipmentsTopic;
    }

    @Scheduled(fixedDelayString = "1000")
    @Transactional
    public void publishBatch() {
        Span span = tracer.spanBuilder("shipping.outbox.publishBatch").startSpan();
        try (Scope scope= span.makeCurrent()) {
            List<OutboxEvent> batch = outboxRepository.findUnpublishedBatch();
            span.setAttribute("outbox.batchSize", batch.size());

            for (OutboxEvent event : batch) {
                outboxKafkaPublisher.publish(shipmentsTopic, event.getAggregateId().toString(), event.getPayload());
                event.markPublished();
            }
        } finally {
            span.end();
        }
    }
}
