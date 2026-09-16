package com.example.trackingservice.service;

import com.example.trackingservice.dto.OrderEvent;
import com.example.trackingservice.model.OrderStatus;
import com.example.trackingservice.model.OrderStatusHistory;

import java.time.Instant;
import java.util.List;

/**
 * Service-layer contract for the Tracking Service.
 *
 * <p>Implementation lives in {@link TrackingServiceImpl}. The contract is split
 * from the implementation so unit tests can mock it without depending on JPA,
 * Redis, or Kafka.
 *
 * <p>The two main entry points are:
 * <ul>
 *   <li>{@link #applyEvent(OrderEvent)} — called by the Kafka consumer for each
 *       incoming event. Idempotent via the {@code processed_events} ledger.</li>
 *   <li>{@link #getTracking(String)} — called by the REST layer for the
 *       {@code GET /api/tracking/orders/{orderId}} endpoint.</li>
 * </ul>
 */
public interface TrackingService {

    /**
     * Apply a single order event idempotently.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Idempotency check: if {@code processed_events.event_id} already exists, skip.</li>
     *   <li>Resolve current status (Redis first, MySQL on miss).</li>
     *   <li>Insert a row into {@code order_status_history} (unique on event_id).</li>
     *   <li>Insert a row into {@code processed_events} (unique on event_id).</li>
     *   <li>Refresh the Redis cache.</li>
     * </ol>
     *
     * @param event the Kafka event payload
     * @return {@code true} if the event was actually applied; {@code false} if it was a
     *         duplicate that was idempotently skipped
     * @throws InvalidStatusTransitionException if the transition is not allowed by the
     *         state machine (the consumer re-throws so the record lands in the DLQ)
     */
    boolean applyEvent(OrderEvent event);

    /**
     * Resolve the latest known status for an order (Redis cache-aside → MySQL fallback).
     *
     * @param orderId business identifier
     * @return the latest status, or {@code null} if no event has ever been applied
     */
    OrderStatus resolveCurrentStatus(String orderId);

    /**
     * Build the tracking snapshot for the REST endpoint:
     * current status, last-updated timestamp, full timeline.
     *
     * @param orderId business identifier
     * @return the snapshot
     * @throws TrackingService.OrderNotFoundException if the order has never been tracked
     */
    TrackingSnapshot getTracking(String orderId);


    final class TrackingSnapshot {
        private final String orderId;
        private final String currentStatus;
        private final Instant lastUpdated;
        private final List<OrderStatusHistory> timeline;

        public TrackingSnapshot(String orderId, String currentStatus, Instant lastUpdated,
                                List<OrderStatusHistory> timeline) {
            this.orderId = orderId;
            this.currentStatus = currentStatus;
            this.lastUpdated = lastUpdated;
            this.timeline = timeline;
        }

        public String getOrderId() { return orderId; }
        public String getCurrentStatus() { return currentStatus; }
        public Instant getLastUpdated() { return lastUpdated; }
        public List<OrderStatusHistory> getTimeline() { return timeline; }
    }

   
    class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String orderId) {
            super("Order not tracked: " + orderId);
        }
    }
}
