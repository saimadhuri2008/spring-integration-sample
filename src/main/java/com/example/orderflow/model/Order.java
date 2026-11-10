package com.example.orderflow.model;

import java.math.BigDecimal;

public record Order(
        String id,
        String customer,
        String description,
        BigDecimal total,
        OrderStatus status) {

    public Order(String id, String customer, String description, BigDecimal total) {
        this(id, customer, description, total, OrderStatus.PENDING);
    }

    public Order withStatus(OrderStatus status) {
        return new Order(id, customer, description, total, status);
    }
}
