package com.example.orderflow.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import com.example.orderflow.model.Order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderValidationServiceTest {

    private OrderValidationService service;

    @BeforeEach
    void setUp() {
        service = new OrderValidationService();
    }

    @Test
    void validOrder_passes() {
        var order = new Order("ORD-001", "Alice", "Book", new BigDecimal("25.00"));
        assertDoesNotThrow(() -> service.validate(order));
    }

    @Test
    void blankId_throws() {
        var order = new Order("", "Alice", "Book", new BigDecimal("25.00"));
        var ex = assertThrows(IllegalArgumentException.class, () -> service.validate(order));
        assertEquals("Order id must not be blank", ex.getMessage());
    }

    @Test
    void nullId_throws() {
        var order = new Order(null, "Alice", "Book", new BigDecimal("25.00"));
        var ex = assertThrows(IllegalArgumentException.class, () -> service.validate(order));
        assertEquals("Order id must not be blank", ex.getMessage());
    }

    @Test
    void blankCustomer_throws() {
        var order = new Order("ORD-001", "", "Book", new BigDecimal("25.00"));
        var ex = assertThrows(IllegalArgumentException.class, () -> service.validate(order));
        assertEquals("Customer must not be blank", ex.getMessage());
    }

    @Test
    void zeroTotal_throws() {
        var order = new Order("ORD-001", "Alice", "Book", BigDecimal.ZERO);
        var ex = assertThrows(IllegalArgumentException.class, () -> service.validate(order));
        assertEquals("Total must be positive", ex.getMessage());
    }

    @Test
    void negativeTotal_throws() {
        var order = new Order("ORD-001", "Alice", "Book", new BigDecimal("-5.00"));
        var ex = assertThrows(IllegalArgumentException.class, () -> service.validate(order));
        assertEquals("Total must be positive", ex.getMessage());
    }

    @Test
    void nullTotal_throws() {
        var order = new Order("ORD-001", "Alice", "Book", null);
        var ex = assertThrows(IllegalArgumentException.class, () -> service.validate(order));
        assertEquals("Total must be positive", ex.getMessage());
    }

    @Test
    void returnsOrder() {
        var order = new Order("ORD-001", "Alice", "Book", new BigDecimal("25.00"));
        var result = service.validate(order);
        assertEquals(order, result);
    }
}
