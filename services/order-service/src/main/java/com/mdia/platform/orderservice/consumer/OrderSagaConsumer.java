package com.mdia.platform.orderservice.consumer;

import com.mdia.platform.orderservice.model.Order;
import com.mdia.platform.orderservice.repo.OrderRepository;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Component
public class OrderSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaConsumer.class);
    private final OrderRepository orderRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderSagaConsumer(OrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }


    @KafkaListener(
            topics = {"${app.kafka.ordersTopic}", "${app.kafka.shippingTopic}"},
            groupId = "order-service"
    )
    @Transactional
    public void onEvent(ConsumerRecord<String, String> record) throws Exception {
        log.info("Received event: {}", record.value());
        Map<?, ?> evt = mapper.readValue(record.value(), Map.class);

        String eventType = (String) evt.get("eventType");
        log.info("event type: {}", eventType);

        Map<?, ?> data = (Map<?, ?>) evt.get("data");

        UUID orderId = null;


        if (data != null && data.get("orderId") != null) {
            orderId = UUID.fromString((String) data.get("orderId"));
        } else if (evt.get("aggregateId") != null) {
            orderId = UUID.fromString((String) evt.get("aggregateId"));
        }

        if (orderId == null) {
            return;
        }



        Order order = orderRepo.findById(orderId).orElse(null);
        log.info("Order {}", order);
        if (order == null) return;

        switch (eventType) {

            case "InventoryReserved" -> order.markInventoryReserved();

            case "ShipmentCreated" -> order.markCompleted();
        }
        log.info("Order {} status now {}", order.getId(), order.getStatus());
    }
}
