package com.example.orderflow.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import com.example.orderflow.model.Order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class JsonToOrderTransformerTest {

    private JsonToOrderTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonToOrderTransformer(JsonMapper.builder().build());
    }

    @Test
    void validJson_returnsOrder() {
        var json = """
            {"id":"ORD-001","customer":"Alice","description":"Book","total":25.00}
            """;
        var result = transformer.transform(json);
        assertEquals(new Order("ORD-001", "Alice", "Book", new BigDecimal("25.00")), result);
    }

    @Test
    void malformedJson_throws() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> transformer.transform("not-json"));
        assertEquals("Failed to parse JSON: not-json", ex.getMessage());
    }

    @Test
    void missingFields_usesNull() {
        var json = """
            {"id":"ORD-001","total":25.00}
            """;
        var result = transformer.transform(json);
        assertEquals("ORD-001", result.id());
        assertEquals(new BigDecimal("25.00"), result.total());
        assertEquals(null, result.customer());
        assertEquals(null, result.description());
    }

    @Test
    void handlesBigDecimal() {
        var json = """
            {"id":"ORD-001","customer":"Alice","description":"Laptop","total":1500.50}
            """;
        var result = transformer.transform(json);
        assertEquals(new BigDecimal("1500.50"), result.total());
    }
}
