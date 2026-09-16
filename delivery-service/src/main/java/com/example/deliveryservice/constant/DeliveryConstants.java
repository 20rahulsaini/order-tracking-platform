package com.example.deliveryservice.constant;

public final class DeliveryConstants {
    private DeliveryConstants() {}
    public static final String EVENT_ID_PREFIX = "EVT-";
    public static final String EVENT_ORDER_SHIPPED = "ORDER_SHIPPED";
    public static final String EVENT_ORDER_OUT_FOR_DELIVERY = "ORDER_OUT_FOR_DELIVERY";
    public static final String EVENT_ORDER_DELIVERED = "ORDER_DELIVERED";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String KAFKA_TOPIC_ORDER_EVENTS = "order-events";
}
