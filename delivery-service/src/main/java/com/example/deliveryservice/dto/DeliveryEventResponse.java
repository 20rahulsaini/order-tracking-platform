package com.example.deliveryservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DeliveryEventResponse", description = "Result of a delivery lifecycle event publication")
public class DeliveryEventResponse {
    private String orderId;
    private String status;
    private String eventType;
    private boolean published;

    public DeliveryEventResponse() {}

    public DeliveryEventResponse(String orderId, String status, String eventType, boolean published) {
        this.orderId = orderId;
        this.status = status;
        this.eventType = eventType;
        this.published = published;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}
