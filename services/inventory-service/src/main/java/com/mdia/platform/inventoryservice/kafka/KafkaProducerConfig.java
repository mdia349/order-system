package com.mdia.platform.inventoryservice.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class KafkaProducerConfig {
    @Bean
    OutboxKafkaPublisher outboxKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        return new OutboxKafkaPublisher(kafkaTemplate);
    }
}
