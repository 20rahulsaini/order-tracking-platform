package com.example.orderservice.service;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;


public interface OrderService {

    /**
     *
     * @param request inbound order payload, already bean-validated
     * @return the persisted order as a {@link OrderResponse}
     */
    OrderResponse createOrder(OrderRequest request);

    /**
     *
     * @param orderId e.g. {@code ORD-ABCD1234}
     * @return the persisted order as a {@link OrderResponse}
     * @throws OrderNotFoundException if no order exists for the given id
     */
    OrderResponse getOrder(String orderId);

    /**
     * Raised when an order cannot be located. Mapped to HTTP 404 by the REST layer.
     */
    class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String orderId) {
            super("Order not found: " + orderId);
        }
    }
}
