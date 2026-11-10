package com.example.orderflow.transformer;

import java.math.BigDecimal;

import com.example.orderflow.model.Order;
import org.springframework.stereotype.Component;

@Component
public class CsvToOrderTransformer {

    public Order transform(String payload) {
        var parts = payload.strip().split(",", -1);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid CSV: expected 4 fields, got " + parts.length);
        }
        var id = parts[0].trim();
        var customer = parts[1].trim();
        var description = parts[2].trim().replaceAll("^\"|\"$", "");
        var total = new BigDecimal(parts[3].trim());
        return new Order(id, customer, description, total);
    }
}
