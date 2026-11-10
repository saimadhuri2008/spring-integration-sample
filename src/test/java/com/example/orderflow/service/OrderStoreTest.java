package com.example.orderflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.example.orderflow.model.Order;
import com.example.orderflow.model.OrderEntity;
import com.example.orderflow.model.OrderStatus;
import com.example.orderflow.web.DuplicateOrderException;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

class OrderStoreTest {

    @Test
    void get_returnsOrderWhenPresent() {
        var repository = mock(OrderRepository.class);
        when(repository.findById("ORD-123")).thenReturn(Optional.of(
                new OrderEntity("ORD-123", "Alice", "Book", new BigDecimal("25.00"), OrderStatus.REVIEWED)));
        var entityManager = mock(EntityManager.class);

        var store = new OrderStore(repository, entityManager);
        Optional<Order> result = store.get("ORD-123");

        assertEquals(Optional.of(new Order("ORD-123", "Alice", "Book", new BigDecimal("25.00"), OrderStatus.REVIEWED)),
                result);
    }

    @Test
    void get_returnsEmptyWhenOrderMissing() {
        var repository = mock(OrderRepository.class);
        when(repository.findById("ORD-404")).thenReturn(Optional.empty());
        var entityManager = mock(EntityManager.class);

        var store = new OrderStore(repository, entityManager);
        Optional<Order> result = store.get("ORD-404");
        assertEquals(Optional.empty(), result);
    }

    @Test
    void put_throwsDuplicateWhenSaveConflicts() {
        var repository = mock(OrderRepository.class);
        when(repository.existsById("ORD-DUP")).thenReturn(false);
        var entityManager = mock(EntityManager.class);
        doThrow(new PersistenceException("duplicate")).when(entityManager).persist(any(OrderEntity.class));

        var store = new OrderStore(repository, entityManager);
        var order = new Order("ORD-DUP", "Alice", "Book", new BigDecimal("25.00"), OrderStatus.PENDING);

        var ex = assertThrows(DuplicateOrderException.class, () -> store.put(order));

        assertEquals("Order already exists: ORD-DUP", ex.getMessage());
    }
}
