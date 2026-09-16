package com.example.trackingservice.constant;

public final class TrackingConstants {
    private TrackingConstants() {}
    public static final String STATUS_PLACED = "PLACED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_PACKED = "PACKED";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String KAFKA_TOPIC_ORDER_EVENTS = "order-events";
    public static final String ERROR_NOT_TRACKED = "NOT_TRACKED";
}
