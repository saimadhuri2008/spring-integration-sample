package com.example.orderflow.service;

import com.example.orderflow.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

    public Order processExpress(Order order) {
        log.info("[{}] Processing EXPRESS order {} for customer {} (${})", MDC.get("correlationId"),
                order.id(), order.customer(), order.total());
        return order.withStatus(com.example.orderflow.model.OrderStatus.PROCESSED);
    }

    public Order processReview(Order order) {
        log.warn("[{}] Order {} for customer {} (${}) flagged for REVIEW - exceeds $100 threshold", MDC.get("correlationId"),
                order.id(), order.customer(), order.total());
        return order.withStatus(com.example.orderflow.model.OrderStatus.REVIEWED);
    }
}
