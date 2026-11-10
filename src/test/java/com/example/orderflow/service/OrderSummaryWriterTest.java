package com.example.orderflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.example.orderflow.model.Order;
import com.example.orderflow.model.OrderStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderSummaryWriterTest {

    @BeforeEach
    void cleanSummaryOutput() throws IOException {
        var dir = Path.of("output/summary");
        if (Files.exists(dir)) {
            try (var walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
        Files.createDirectories(dir);
    }

    @Test
    void write_appendsOrdersAndKeepsSingleHeader() throws Exception {
        var writer = new OrderSummaryWriter();
        var orders1 = List.of(
                new Order("ORD-A1", "Alice", "Book", BigDecimal.valueOf(25), OrderStatus.PROCESSED),
                new Order("ORD-A2", "Bob", "Lamp", BigDecimal.valueOf(50), OrderStatus.PROCESSED));
        var orders2 = List.of(
                new Order("ORD-A3", "Charlie", "Desk", BigDecimal.valueOf(75), OrderStatus.PROCESSED));

        var path = writer.write(orders1, "batch-A");
        writer.write(orders2, "batch-A");

        var content = Files.readString(Path.of(path));
        assertThat(content).contains("ORD-A1").contains("ORD-A2").contains("ORD-A3");
        assertThat(countOccurrences(content, "Date,OrderID,Customer,Description,Total")).isEqualTo(1);
    }

    private static int countOccurrences(String content, String value) {
        var count = 0;
        var offset = 0;
        while (true) {
            var index = content.indexOf(value, offset);
            if (index < 0) {
                return count;
            }
            count++;
            offset = index + value.length();
        }
    }
}
