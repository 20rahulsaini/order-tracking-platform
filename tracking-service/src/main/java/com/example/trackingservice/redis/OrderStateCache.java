package com.example.trackingservice.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Thin wrapper around {@link StringRedisTemplate} that stores the latest
 * tracking state per order as JSON.
 *
 * Cache key:    {@code order:ORD-1001}
 * Cache value:  JSON: {"orderId", "status", "eventType", "lastEventId", "lastUpdated"}
 *
 * Notes:
 *   - Cache is read-through (via TrackingService fallback to MySQL).
 *   - Cache has a TTL (default 1h) to avoid unbounded growth.
 *   - Cache is invalidated/refreshed every time a new event is applied.
 *   - On Redis MISS the caller falls back to MySQL and then writes back here.
 */
@Component
public class OrderStateCache {

    private static final Logger log = LoggerFactory.getLogger(OrderStateCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public OrderStateCache(StringRedisTemplate redis,
                           ObjectMapper objectMapper,
                           @Value("${tracking.redis.ttl-seconds:3600}") long ttlSeconds) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    private String keyFor(String orderId) {
        return "order:" + orderId;
    }

    /** Returns the cached state, or {@code null} on a miss (or Redis error). */
    public CachedOrderState get(String orderId) {
        try {
            String json = redis.opsForValue().get(keyFor(orderId));
            if (json == null) {
                log.debug("Redis MISS for {}", keyFor(orderId));
                return null;
            }
            log.debug("Redis HIT for {}", keyFor(orderId));
            return objectMapper.readValue(json, CachedOrderState.class);
        } catch (Exception e) {
            log.warn("Redis read failed for {}: {}", keyFor(orderId), e.getMessage());
            return null;  // degrade to MySQL
        }
    }

    public void put(CachedOrderState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            redis.opsForValue().set(keyFor(state.getOrderId()), json, ttl);
        } catch (Exception e) {
            log.warn("Redis write failed for {}: {}", keyFor(state.getOrderId()), e.getMessage());
            // swallow - cache failure must not fail the request
        }
    }

    public void evict(String orderId) {
        try {
            redis.delete(keyFor(orderId));
        } catch (Exception e) {
            log.warn("Redis evict failed for {}: {}", keyFor(orderId), e.getMessage());
        }
    }

    /**
     * Snapshot of the latest known state for an order.
     */
    public static final class CachedOrderState {
        private String orderId;
        private String status;
        private String lastEventType;
        private String lastEventId;
        private java.time.Instant lastUpdated;

        public CachedOrderState() {
        }

        public CachedOrderState(String orderId, String status, String lastEventType,
                                String lastEventId, java.time.Instant lastUpdated) {
            this.orderId = orderId;
            this.status = status;
            this.lastEventType = lastEventType;
            this.lastEventId = lastEventId;
            this.lastUpdated = lastUpdated;
        }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getLastEventType() { return lastEventType; }
        public void setLastEventType(String lastEventType) { this.lastEventType = lastEventType; }

        public String getLastEventId() { return lastEventId; }
        public void setLastEventId(String lastEventId) { this.lastEventId = lastEventId; }

        public java.time.Instant getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(java.time.Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}
