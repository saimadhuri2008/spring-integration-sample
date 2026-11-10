package com.example.orderflow.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(DuplicateOrderException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DuplicateOrderException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception e) {
        log.debug("Exception type: {}", e.getClass().getName());
        var message = resolveMessage(e);
        var status = resolveStatus(e);
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }

    private static String resolveMessage(Throwable e) {
        if (e.getCause() != null) {
            var deeper = e.getCause();
            if (deeper.getCause() != null
                    && !(deeper instanceof MessagingException)
                    && !(deeper instanceof MessageDeliveryException)) {
                return deeper.getCause().getMessage() != null ? deeper.getCause().getMessage() : deeper.getMessage();
            }
            return deeper.getMessage() != null ? deeper.getMessage() : e.getMessage();
        }
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
    }

    private static HttpStatus resolveStatus(Throwable e) {
        var cause = e;
        while (cause != null) {
            if (cause instanceof IllegalArgumentException) {
                return HttpStatus.BAD_REQUEST;
            }
            if (cause instanceof DuplicateOrderException) {
                return HttpStatus.CONFLICT;
            }
            cause = cause.getCause();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
