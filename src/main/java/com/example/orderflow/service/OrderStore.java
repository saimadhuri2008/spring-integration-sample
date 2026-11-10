package com.example.orderflow.service;

import java.util.Optional;
import java.util.List;

import com.example.orderflow.model.Order;
import com.example.orderflow.model.OrderEntity;
import com.example.orderflow.model.OrderStatus;
import com.example.orderflow.web.DuplicateOrderException;
import com.example.orderflow.web.OrderNotFoundException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderStore {

    private final OrderRepository repository;
    private final EntityManager entityManager;

    public OrderStore(OrderRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Transactional
    public Order put(Order order) {
        if (repository.existsById(order.id())) {
            throw new DuplicateOrderException(order.id());
        }
        try {
            entityManager.persist(OrderEntity.fromOrder(order));
            entityManager.flush();
        } catch (PersistenceException ex) {
            throw new DuplicateOrderException(order.id());
        }
        return order;
    }

    public Optional<Order> get(String id) {
        return repository.findById(id).map(OrderEntity::toOrder);
    }

    public List<Order> listAll() {
        return repository.findAll().stream().map(OrderEntity::toOrder).toList();
    }

    public Order findOne(String id) {
        return get(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public int count() {
        return (int) repository.count();
    }

    public void markFailed(String id) {
        repository.findById(id).ifPresent(entity -> {
            entity.setStatus(OrderStatus.FAILED);
            repository.save(entity);
        });
    }
}
