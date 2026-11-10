package com.example.orderflow.web;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Gateway;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

@MessagingGateway(defaultRequestChannel = "httpOrderInputChannel")
public interface OrderGateway {
    String process(@Payload String body, @Header(value = "summaryBatch", required = false) String summaryBatch);

    @Gateway(requestChannel = "getOrderInputChannel")
    com.example.orderflow.model.Order get(@Payload String id);
}
