package com.example.orderflow.service;

import java.math.BigDecimal;

import com.example.orderflow.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class OrderValidationService {

    private static final Logger log = LoggerFactory.getLogger(OrderValidationService.class);

    public Order validate(Order order) {
        if (order.id() == null || order.id().isBlank()) {
            throw new IllegalArgumentException("Order id must not be blank");
        }
        if (order.customer() == null || order.customer().isBlank()) {
            throw new IllegalArgumentException("Customer must not be blank");
        }
        if (order.total() == null || order.total().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total must be positive");
        }
        log.info("[{}] Order {} validated successfully", MDC.get("correlationId"), order.id());
        return order;
    }
}
