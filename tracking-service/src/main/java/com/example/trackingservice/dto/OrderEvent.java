package com.example.trackingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class OrderEvent {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("status")
    private String status;

    @JsonProperty("timestamp")
    private Instant timestamp;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "OrderEvent{eventId='" + eventId + "', orderId='" + orderId
                + "', eventType='" + eventType + "', status='" + status
                + "', timestamp=" + timestamp + '}';
    }

    public OrderEvent() {
    }

    public OrderEvent(
        String eventId,
        String orderId,
        String eventType,
        String status,
        Instant timestamp) {
    this.eventId = eventId;
    this.orderId = orderId;
    this.eventType = eventType;
    this.status = status;
    this.timestamp = timestamp;
   }
}
