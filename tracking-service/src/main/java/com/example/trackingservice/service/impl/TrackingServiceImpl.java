package com.example.trackingservice.service;

import com.example.trackingservice.constant.TrackingConstants;
import com.example.trackingservice.dao.DaoResult;
import com.example.trackingservice.dao.OrderStatusHistoryDao;
import com.example.trackingservice.dao.ProcessedEventDao;
import com.example.trackingservice.dto.OrderEvent;
import com.example.trackingservice.model.OrderStatus;
import com.example.trackingservice.model.OrderStatusHistory;
import com.example.trackingservice.model.ProcessedEvent;
import com.example.trackingservice.redis.OrderStateCache;
import com.example.trackingservice.redis.OrderStateCache.CachedOrderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TrackingServiceImpl implements TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingServiceImpl.class);

    private final OrderStatusHistoryDao historyDao;
    private final ProcessedEventDao processedEventDao;
    private final OrderStateCache cache;

    public TrackingServiceImpl(OrderStatusHistoryDao historyDao,
                               ProcessedEventDao processedEventDao,
                               OrderStateCache cache) {
        this.historyDao = historyDao;
        this.processedEventDao = processedEventDao;
        this.cache = cache;
    }

    @Override
    @Transactional
    public boolean applyEvent(OrderEvent event) {
        log.info("Applying event eventId={} orderId={} eventType={} status={}",
                event.getEventId(), event.getOrderId(), event.getEventType(), event.getStatus());

        DaoResult<ProcessedEvent> processedResult =
                processedEventDao.findByEventId(event.getEventId());
        if (processedResult.isFound()) {
            log.info("Event {} already processed, skipping", event.getEventId());
            return false;
        }

        OrderStatus currentStatus = resolveCurrentStatus(event.getOrderId());
        OrderStatus targetStatus = parseStatus(event);

        if (currentStatus == null) {
            if (targetStatus != OrderStatus.PLACED) {
                throw new InvalidStatusTransitionException(
                        event.getOrderId(), "<none>", targetStatus.name());
            }
        } else {
            OrderStatusStateMachine.validateTransition(currentStatus, targetStatus);
        }

        Instant now = Instant.now();
        OrderStatusHistory history = new OrderStatusHistory(
                event.getOrderId(),
                targetStatus.name(),
                event.getEventType(),
                event.getEventId(),
                event.getTimestamp() != null ? event.getTimestamp() : now,
                now
        );

        try {
            historyDao.save(history);
            processedEventDao.save(new ProcessedEvent(
                    event.getEventId(),
                    event.getOrderId(),
                    targetStatus.name(),
                    now
            ));
        } catch (DataIntegrityViolationException duplicate) {
            log.warn("Duplicate event {} detected during persistence; skipping",
                    event.getEventId());
            return false;
        }

        cache.put(new CachedOrderState(
                event.getOrderId(),
                targetStatus.name(),
                event.getEventType(),
                event.getEventId(),
                history.getOccurredAt()
        ));

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStatus resolveCurrentStatus(String orderId) {
        CachedOrderState cached = cache.get(orderId);
        if (cached != null) {
            OrderStatus status = safeParse(cached.getStatus());
            if (status != null) {
                return status;
            }
            cache.evict(orderId);
        }

        DaoResult<OrderStatusHistory> latestResult = historyDao.findLatestByOrderId(orderId);
        if (!latestResult.isFound()) {
            return null;
        }
        return parseStatus(latestResult.getData().getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingSnapshot getTracking(String orderId) {
        CachedOrderState cached = cache.get(orderId);
        OrderStatus currentStatus;
        Instant lastUpdated;

        if (cached != null && safeParse(cached.getStatus()) != null) {
            currentStatus = safeParse(cached.getStatus());
            lastUpdated = cached.getLastUpdated();
        } else {
            DaoResult<OrderStatusHistory> latestResult =
                    historyDao.findLatestByOrderId(orderId);
            if (!latestResult.isFound()) {
                throw new OrderNotFoundException(orderId);
            }

            OrderStatusHistory latest = latestResult.getData();
            currentStatus = parseStatus(latest.getStatus());
            lastUpdated = latest.getOccurredAt();

            cache.put(new CachedOrderState(
                    orderId,
                    currentStatus.name(),
                    latest.getEventType(),
                    latest.getEventId(),
                    latest.getOccurredAt()
            ));
        }

        DaoResult<List<OrderStatusHistory>> timelineResult =
                historyDao.findTimelineByOrderId(orderId);

        return new TrackingSnapshot(
                orderId,
                currentStatus.name(),
                lastUpdated,
                timelineResult.getData()
        );
    }

    private OrderStatus safeParse(String value) {
        try {
            return OrderStatus.valueOf(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private OrderStatus parseStatus(String value) {
        try {
            return OrderStatus.valueOf(value);
        } catch (Exception ex) {
            throw new InvalidStatusTransitionException(null, "<unknown>", value);
        }
    }

    private OrderStatus parseStatus(OrderEvent event) {
        try {
            return OrderStatus.valueOf(event.getStatus());
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Unknown status in event " + event.getEventId() + ": " + event.getStatus(), ex);
        }
    }
}
