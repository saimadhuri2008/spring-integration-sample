package com.example.orderflow.transformer;

import com.example.orderflow.model.Order;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JsonToOrderTransformer {

    private final JsonMapper jsonMapper;

    public JsonToOrderTransformer(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public Order transform(String payload) {
        try {
            var order = jsonMapper.readValue(payload, Order.class);
            return order.status() != null ? order : order.withStatus(com.example.orderflow.model.OrderStatus.PENDING);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON: " + payload, e);
        }
    }
}
