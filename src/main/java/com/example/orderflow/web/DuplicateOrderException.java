package com.example.orderflow.web;

public class DuplicateOrderException extends RuntimeException {

    public DuplicateOrderException(String id) {
        super("Order already exists: " + id);
    }
}
