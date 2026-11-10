package com.example.orderflow.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import com.example.orderflow.model.Order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CsvToOrderTransformerTest {

    private CsvToOrderTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new CsvToOrderTransformer();
    }

    @Test
    void validCsv_returnsOrder() {
        var result = transformer.transform("ORD-001,Alice,Book,25.00");
        assertEquals(new Order("ORD-001", "Alice", "Book", new BigDecimal("25.00")), result);
    }

    @Test
    void trimsWhitespace() {
        var result = transformer.transform("  ORD-001 ,  Alice , Book , 25.00  ");
        assertEquals(new Order("ORD-001", "Alice", "Book", new BigDecimal("25.00")), result);
    }

    @Test
    void stripsQuotesFromDescription() {
        var result = transformer.transform("ORD-001,Alice,\"My Book\",25.00");
        assertEquals(new Order("ORD-001", "Alice", "My Book", new BigDecimal("25.00")), result);
    }

    @Test
    void tooFewFields_throws() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> transformer.transform("ORD-001,Alice"));
        assertEquals("Invalid CSV: expected 4 fields, got 2", ex.getMessage());
    }

    @Test
    void handlesNewline() {
        var result = transformer.transform("ORD-001,Alice,Book,25.00\n");
        assertEquals(new Order("ORD-001", "Alice", "Book", new BigDecimal("25.00")), result);
    }
}
