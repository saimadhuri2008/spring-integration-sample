package com.example.orderflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;

import com.example.orderflow.model.Order;
import com.example.orderflow.model.OrderStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderProcessingServiceTest {

    private OrderProcessingService service;

    @BeforeEach
    void setUp() {
        service = new OrderProcessingService();
    }

    @Test
    void processExpress_returnsProcessedOrder() {
        var order = new Order("ORD-001", "Alice", "Book", new BigDecimal("25.00"));
        var result = service.processExpress(order);
        assertEquals(OrderStatus.PROCESSED, result.status());
        assertEquals("ORD-001", result.id());
    }

    @Test
    void processReview_returnsProcessedOrder() {
        var order = new Order("ORD-002", "Bob", "Laptop", new BigDecimal("1500.00"));
        var result = service.processReview(order);
        assertEquals(OrderStatus.REVIEWED, result.status());
        assertEquals("ORD-002", result.id());
    }
}
