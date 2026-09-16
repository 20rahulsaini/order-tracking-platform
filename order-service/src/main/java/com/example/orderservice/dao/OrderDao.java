package com.example.orderservice.dao;

import com.example.orderservice.model.Order;

public interface OrderDao {
    DaoResult<Order> save(Order order);
    DaoResult<Order> findByOrderId(String orderId);
}
