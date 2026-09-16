package com.example.orderservice.constant;

public final class OrderConstants {
    private OrderConstants() {}
    public static final String ORDER_ID_PREFIX = "ORD-";
    public static final String EVENT_ID_PREFIX = "EVT-";
    public static final String STATUS_PLACED = "PLACED";
    public static final String EVENT_ORDER_PLACED = "ORDER_PLACED";
    public static final String ERROR_NOT_FOUND = "NOT_FOUND";
    public static final String KAFKA_TOPIC_ORDER_EVENTS = "order-events";
}
