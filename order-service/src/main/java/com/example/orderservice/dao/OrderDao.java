package com.example.orderservice.dao;

import com.example.orderservice.model.Order;
import java.util.Optional;
import java.util.List;

public interface OrderDao {
   public DaoResult<Order> save(Order order);

   public Order findByOrderId(String orderId);

   public List<Order> findAllOrders();
}
