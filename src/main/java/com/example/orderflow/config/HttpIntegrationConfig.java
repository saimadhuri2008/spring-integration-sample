package com.example.orderflow.config;

import com.example.orderflow.model.Order;
import com.example.orderflow.service.OrderProcessingService;
import com.example.orderflow.service.OrderStore;
import com.example.orderflow.service.OrderValidationService;
import com.example.orderflow.transformer.JsonToOrderTransformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.LoggingHandler;

import java.util.UUID;

@Configuration
public class HttpIntegrationConfig {

    private static final Logger log = LoggerFactory.getLogger(HttpIntegrationConfig.class);

    private final OrderProperties orderProperties;

    public HttpIntegrationConfig(OrderProperties orderProperties) {
        this.orderProperties = orderProperties;
    }

    @Bean
    public IntegrationFlow httpOrderFlow(
            JsonToOrderTransformer jsonTransformer,
            OrderValidationService validator,
            OrderProcessingService processor,
            OrderStore orderStore) {

        return IntegrationFlow.from("httpOrderInputChannel")
            .enrichHeaders(h -> h.headerFunction("correlationId", m -> UUID.randomUUID().toString()))
            .log(LoggingHandler.Level.INFO, "orderflow.http")
            .transform(jsonTransformer)
            .handle(validator, "validate")
            .route(Order.class, this::isExpressOrder,
                m -> m
                    .subFlowMapping(true, sf -> sf
                        .handle(processor, "processExpress")
                    )
                    .subFlowMapping(false, sf -> sf
                        .handle(processor, "processReview")
                    )
            )
            .handle(orderStore, "put")
            .publishSubscribeChannel(pubSub -> pubSub
                .subscribe(sub -> sub.channel("archiveChannel"))
                .subscribe(sub -> sub.channel("summaryChannel"))
                .subscribe(sub -> sub
                    .<Order, String>transform(o -> "Order " + o.id() + " submitted ("
                        + (isExpressOrder(o) ? "EXPRESS" : "REVIEW") + ")"))
            )
            .get();
    }

    @Bean
    public IntegrationFlow getOrderFlow(OrderStore orderStore) {
        return IntegrationFlow.from("getOrderInputChannel")
            .handle(orderStore, "findOne")
            .get();
    }

    private boolean isExpressOrder(Order order) {
        return order.total().compareTo(orderProperties.getRouting().getExpressThreshold()) <= 0;
    }
}
