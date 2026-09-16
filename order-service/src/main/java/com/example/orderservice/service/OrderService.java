package com.example.orderservice.service;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import java.util.List;

public interface OrderService {

   public OrderResponse createOrder(OrderRequest request);

   public OrderResponse getOrder(String orderId);

   public List<OrderResponse> getAllOrder();

    class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String orderId) {
            super("Order not found: " + orderId);
        }
    }
}
