package com.example.orderflow.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import com.example.orderflow.model.Order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class OrderSummaryWriter {

    private static final Logger log = LoggerFactory.getLogger(OrderSummaryWriter.class);

    public String write(List<Order> orders, @Header("summaryKey") String summaryKey) {
        if (orders == null || orders.isEmpty()) {
            return "";
        }

        var file = Path.of("output/summary", "summary-" + summaryKey + ".csv");
        try {
            Files.createDirectories(file.getParent());
            var needsHeader = !Files.exists(file) || Files.size(file) == 0;
            try (var writer = Files.newBufferedWriter(file, UTF_8, CREATE, APPEND)) {
                if (needsHeader) {
                    writer.write("Date,OrderID,Customer,Description,Total");
                    writer.newLine();
                }
                for (var order : orders) {
                    writer.write(LocalDate.now() + "," + order.id() + "," + order.customer() + ","
                            + (order.description() != null ? order.description() : "") + "," + order.total());
                    writer.newLine();
                }
            }
            log.info("Wrote summary file {} for key {} with {} orders", file, summaryKey, orders.size());
            return file.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write summary file " + file, ex);
        }
    }
}
