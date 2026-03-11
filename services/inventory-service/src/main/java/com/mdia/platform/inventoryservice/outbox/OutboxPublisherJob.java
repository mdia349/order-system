package com.mdia.platform.inventoryservice.outbox;

import com.mdia.platform.inventoryservice.entity.OutboxEvent;
import com.mdia.platform.inventoryservice.kafka.OutboxKafkaPublisher;
import com.mdia.platform.inventoryservice.repo.OutboxRepository;
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
    private final String ordersTopic;
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("inventory-service");

    public OutboxPublisherJob(
            OutboxRepository outboxRepository,
            OutboxKafkaPublisher outboxKafkaPublisher,
            @Value("${app.kafka.ordersTopic}") String ordersTopic
    ) {
        this.outboxRepository = outboxRepository;
        this.outboxKafkaPublisher = outboxKafkaPublisher;
        this.ordersTopic = ordersTopic;
    }

    @Scheduled(fixedDelayString = "1000")
    @Transactional
    public void publishBatch() {
        Span span = tracer.spanBuilder("inventory.outbox.publishBatch").startSpan();
        try (Scope scope = span.makeCurrent()) {
            List<OutboxEvent> batch = outboxRepository.findUnpublishedBatch();
            span.setAttribute("outbox.batchSize", batch.size());

            for (OutboxEvent event : batch) {
                outboxKafkaPublisher.publish(ordersTopic, event.getAggregateId().toString(), event.getPayload());
                event.markPublished();
            }
        } finally {
            span.end();
        }
    }
}
