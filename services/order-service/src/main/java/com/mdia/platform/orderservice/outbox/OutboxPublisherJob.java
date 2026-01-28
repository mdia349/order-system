package com.mdia.platform.orderservice.outbox;

import com.mdia.platform.orderservice.entity.OutboxEvent;
import com.mdia.platform.orderservice.kafka.OutboxKafkaPublisher;
import com.mdia.platform.orderservice.repo.OutboxRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisherJob {

    private final OutboxRepository outbox;
    private final OutboxKafkaPublisher publisher;
    private final String ordersTopic;

    public OutboxPublisherJob(
            OutboxRepository outbox,
            OutboxKafkaPublisher publisher,
            @Value("${app.kafka.ordersTopic}") String ordersTopic
    ) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.ordersTopic = ordersTopic;
    }

    @Scheduled(fixedDelayString = "1000")
    @Transactional
    public void publishBatch() {
        List<OutboxEvent> batch = outbox.findUnpublishedBatch();
        for (OutboxEvent event: batch) {
            publisher.publish(ordersTopic, event.getAggregateId().toString(), event.getPayload());
            event.markPublished();
        }
    }
}
