package com.example.trackingservice.dto;

import com.example.trackingservice.model.OrderStatusHistory;
import com.example.trackingservice.service.TrackingService.TrackingSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "TrackingResponse", description = "Order tracking snapshot")
public class TrackingResponse {
    private String orderId;
    private String currentStatus;
    private Instant lastUpdated;
    private List<TimelineEntry> timeline;

    public static TrackingResponse from(TrackingSnapshot snapshot) {
        TrackingResponse response = new TrackingResponse();
        response.orderId = snapshot.getOrderId();
        response.currentStatus = snapshot.getCurrentStatus();
        response.lastUpdated = snapshot.getLastUpdated();
        response.timeline = snapshot.getTimeline().stream()
                .map(TimelineEntry::from)
                .toList();
        return response;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    public List<TimelineEntry> getTimeline() { return timeline; }
    public void setTimeline(List<TimelineEntry> timeline) { this.timeline = timeline; }

    @Schema(name = "TimelineEntry", description = "A single status-change entry")
    public static class TimelineEntry {
        private String status;
        private String eventType;
        private String eventId;
        private Instant occurredAt;
        private Instant recordedAt;

        public static TimelineEntry from(OrderStatusHistory history) {
            TimelineEntry entry = new TimelineEntry();
            entry.status = history.getStatus();
            entry.eventType = history.getEventType();
            entry.eventId = history.getEventId();
            entry.occurredAt = history.getOccurredAt();
            entry.recordedAt = history.getRecordedAt();
            return entry;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        public Instant getOccurredAt() { return occurredAt; }
        public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
        public Instant getRecordedAt() { return recordedAt; }
        public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
    }
}
