package com.example.orderflow.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ErrorHandlerTest {

    private final ErrorHandler handler = new ErrorHandler();

    @Test
    void handleNotFound_returns404AndErrorBody() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(new OrderNotFoundException("ORD-1"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Order not found: ORD-1", response.getBody().error());
    }

    @Test
    void handleAnyWithIllegalArgumentException_returns400() {
        var exception = new RuntimeException("outer", new IllegalArgumentException("bad order"));

        ResponseEntity<ErrorResponse> response = handler.handleAny(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad order", response.getBody().error());
    }

    @Test
    void handleAnyWithNestedCause_usesDeepestMessage() {
        var exception = new RuntimeException("outer",
                new RuntimeException("middle", new IllegalArgumentException("too small")));

        ResponseEntity<ErrorResponse> response = handler.handleAny(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("too small", response.getBody().error());
    }

    @Test
    void handleConflict_returns409() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(new DuplicateOrderException("ORD-9"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Order already exists: ORD-9", response.getBody().error());
    }
}
