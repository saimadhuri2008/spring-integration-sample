package com.example.orderflow.model;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(nullable = false, unique = true)
    private String id;
    private String customer;
    private String description;
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    public OrderEntity() {
    }

    public OrderEntity(String id, String customer, String description, BigDecimal total, OrderStatus status) {
        this.id = id;
        this.customer = customer;
        this.description = description;
        this.total = total;
        this.status = status;
    }

    public static OrderEntity fromOrder(Order order) {
        return new OrderEntity(order.id(), order.customer(), order.description(), order.total(), order.status());
    }

    public Order toOrder() {
        return new Order(id, customer, description, total, status);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
