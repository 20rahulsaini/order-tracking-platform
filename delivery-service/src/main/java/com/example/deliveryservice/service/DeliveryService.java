package com.example.deliveryservice.service;


public interface DeliveryService {

    /**
     * Emit an {@code ORDER_SHIPPED} / {@code SHIPPED} event for the given order.
     *
     * @param orderId the business identifier of the order to ship
     */
    void ship(String orderId);

    /**
     * Emit an {@code ORDER_OUT_FOR_DELIVERY} / {@code OUT_FOR_DELIVERY} event for the given order.
     *
     * @param orderId the business identifier of the order
     */
    void outForDelivery(String orderId);

    /**
     * Emit an {@code ORDER_DELIVERED} / {@code DELIVERED} event for the given order.
     *
     * @param orderId the business identifier of the order
     */
    void deliver(String orderId);
}
