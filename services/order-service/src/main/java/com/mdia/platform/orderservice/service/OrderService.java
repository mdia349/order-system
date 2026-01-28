package com.mdia.platform.orderservice.service;

import com.mdia.platform.orderservice.entity.Order;
import com.mdia.platform.orderservice.entity.OutboxEvent;
import com.mdia.platform.orderservice.repo.OrderRepository;
import com.mdia.platform.orderservice.repo.OutboxRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderService(OrderRepository orderRepo, OutboxRepository outboxRepo) {
        this.orderRepo = orderRepo;
        this.outboxRepo = outboxRepo;
    }

    @Transactional
    public Order createOrder(UUID userId) {
        UUID orderId = UUID.randomUUID();

        Order order = new Order(orderId, userId, "CREATED");
        orderRepo.save(order);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "eventType", "OrderCreated",
                    "aggregateId", orderId.toString(),
                    "occurredAt", Instant.now().toString(),
                    "userId", userId.toString()
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        outboxRepo.save(new OutboxEvent(
                UUID.randomUUID(),
                "Order",
                orderId,
                "OrderCreated",
                payload,
                Instant.now()
        ));

        return order;
    }
}
