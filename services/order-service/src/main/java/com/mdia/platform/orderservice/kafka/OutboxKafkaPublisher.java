package com.mdia.platform.orderservice.kafka;


import org.springframework.kafka.core.KafkaTemplate;

public class OutboxKafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, String payload) {
        kafkaTemplate.send(topic, key, payload);
    }
}
