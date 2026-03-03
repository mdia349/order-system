package com.mdia.platform.orderservice.controller;

import com.mdia.platform.orderservice.model.Order;
import com.mdia.platform.orderservice.repo.OrderRepository;
import com.mdia.platform.orderservice.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepo;

    private OrderController(OrderService orderService, OrderRepository orderRepo) {
        this.orderService = orderService;
        this.orderRepo = orderRepo;
    }

    @PostMapping
    public Map<String, Object> create(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        Order order = orderService.createOrder(userId);
        return Map.of("orderId", order.getId(), "status", order.getStatus());
    }

    @GetMapping
    private List<Order> myOrders(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return orderRepo.findByUserId(userId);
    }
}
