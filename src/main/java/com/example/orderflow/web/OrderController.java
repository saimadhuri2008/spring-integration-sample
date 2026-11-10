package com.example.orderflow.web;

import java.util.List;

import com.example.orderflow.model.Order;
import com.example.orderflow.service.OrderStore;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderStore orderStore;
    private final OrderGateway orderGateway;

    public OrderController(OrderStore orderStore, OrderGateway orderGateway) {
        this.orderStore = orderStore;
        this.orderGateway = orderGateway;
    }

    @GetMapping("/api/orders")
    public List<Order> listOrders() {
        return orderStore.listAll();
    }

    @GetMapping("/api/orders/{id}")
    public Order getOrder(@PathVariable String id) {
        return orderGateway.get(id);
    }

    @PostMapping("/api/orders")
    public String createOrder(@RequestBody String body, @RequestHeader(name = "summaryBatch", required = false) String summaryBatch) {
        return orderGateway.process(body, summaryBatch);
    }
}
