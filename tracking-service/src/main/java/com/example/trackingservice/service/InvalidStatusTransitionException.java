package com.example.trackingservice.service;


public class InvalidStatusTransitionException extends RuntimeException {
    private final String orderId;
    private final String current;
    private final String target;

    public InvalidStatusTransitionException(String orderId, String current, String target) {
        super("Invalid transition for order " + orderId + ": " + current + " -> " + target);
        this.orderId = orderId;
        this.current = current;
        this.target = target;
    }

    public String getOrderId() { return orderId; }
    public String getCurrent() { return current; }
    public String getTarget() { return target; }
}
