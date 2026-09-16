package com.example.trackingservice.service;

import com.example.trackingservice.dao.DaoResult;
import com.example.trackingservice.dao.OrderStatusHistoryDao;
import com.example.trackingservice.dao.ProcessedEventDao;
import com.example.trackingservice.dto.OrderEvent;
import com.example.trackingservice.model.OrderStatus;
import com.example.trackingservice.model.OrderStatusHistory;
import com.example.trackingservice.model.ProcessedEvent;
import com.example.trackingservice.redis.OrderStateCache;
import com.example.trackingservice.redis.OrderStateCache.CachedOrderState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingServiceImplTest {

    @Mock private OrderStatusHistoryDao historyDao;
    @Mock private ProcessedEventDao processedEventDao;
    @Mock private OrderStateCache cache;
    @InjectMocks private TrackingServiceImpl trackingService;

    private OrderEvent placed() {
        return new OrderEvent("EVT-001", "ORD-1", "ORDER_PLACED", "PLACED",
                Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void applyEvent_firstEvent_persistsHistoryAndLedger() {
        OrderEvent event = placed();
        when(processedEventDao.findByEventId(event.getEventId()))
                .thenReturn(DaoResult.notFound("missing"));
        when(cache.get("ORD-1")).thenReturn(null);
        when(historyDao.findLatestByOrderId("ORD-1"))
                .thenReturn(DaoResult.notFound("missing"));
        when(historyDao.save(any())).thenAnswer(i -> DaoResult.success(i.getArgument(0), "saved"));
        when(processedEventDao.save(any())).thenAnswer(i -> DaoResult.success(i.getArgument(0), "saved"));

        assertThat(trackingService.applyEvent(event)).isTrue();

        verify(historyDao).save(any(OrderStatusHistory.class));
        verify(processedEventDao).save(any(ProcessedEvent.class));
        verify(cache).put(any(CachedOrderState.class));
    }

    @Test
    void applyEvent_duplicate_isSkipped() {
        when(processedEventDao.findByEventId("EVT-001"))
                .thenReturn(DaoResult.found(new ProcessedEvent(
                        "EVT-001", "ORD-1", "PLACED", Instant.now())));

        assertThat(trackingService.applyEvent(placed())).isFalse();

        verify(historyDao, never()).save(any());
        verify(processedEventDao, never()).save(any());
        verify(cache, never()).put(any());
    }

    @Test
    void applyEvent_invalidTransition_isRejected() {
        OrderEvent event = new OrderEvent(
                "EVT-002", "ORD-1", "ORDER_SHIPPED", "SHIPPED", Instant.now());
        when(processedEventDao.findByEventId("EVT-002"))
                .thenReturn(DaoResult.notFound("missing"));
        when(cache.get("ORD-1"))
                .thenReturn(new CachedOrderState(
                        "ORD-1", "PLACED", "ORDER_PLACED", "EVT-001", Instant.now()));

        assertThatThrownBy(() -> trackingService.applyEvent(event))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(historyDao, never()).save(any());
        verify(processedEventDao, never()).save(any());
    }

    @Test
    void applyEvent_duplicateDetectedByDatabase_isSkipped() {
        OrderEvent event = placed();
        when(processedEventDao.findByEventId("EVT-001"))
                .thenReturn(DaoResult.notFound("missing"));
        when(cache.get("ORD-1")).thenReturn(null);
        when(historyDao.findLatestByOrderId("ORD-1"))
                .thenReturn(DaoResult.notFound("missing"));
        when(historyDao.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThat(trackingService.applyEvent(event)).isFalse();
        verify(processedEventDao, never()).save(any());
        verify(cache, never()).put(any());
    }

    @Test
    void getTracking_cacheHit_usesTimelineDao() {
        CachedOrderState cached = new CachedOrderState(
                "ORD-1", "SHIPPED", "ORDER_SHIPPED", "EVT-9",
                Instant.parse("2024-01-01T05:00:00Z"));
        when(cache.get("ORD-1")).thenReturn(cached);
        when(historyDao.findTimelineByOrderId("ORD-1"))
                .thenReturn(DaoResult.success(List.of(), "timeline"));

        TrackingService.TrackingSnapshot snapshot = trackingService.getTracking("ORD-1");

        assertThat(snapshot.getCurrentStatus()).isEqualTo("SHIPPED");
        assertThat(snapshot.getLastUpdated())
                .isEqualTo(Instant.parse("2024-01-01T05:00:00Z"));
        verify(historyDao, never()).findLatestByOrderId(anyString());
    }

    @Test
    void getTracking_cacheMiss_readsLatestAndTimeline() {
        when(cache.get("ORD-1")).thenReturn(null);
        OrderStatusHistory latest = new OrderStatusHistory(
                "ORD-1", "PACKED", "ORDER_PACKED", "EVT-7",
                Instant.parse("2024-01-01T03:00:00Z"),
                Instant.parse("2024-01-01T03:00:05Z"));
        when(historyDao.findLatestByOrderId("ORD-1")).thenReturn(DaoResult.found(latest));
        when(historyDao.findTimelineByOrderId("ORD-1"))
                .thenReturn(DaoResult.success(List.of(latest), "timeline"));

        TrackingService.TrackingSnapshot snapshot = trackingService.getTracking("ORD-1");

        assertThat(snapshot.getCurrentStatus()).isEqualTo("PACKED");
        verify(cache).put(any(CachedOrderState.class));
    }

    @Test
    void getTracking_unknownOrder_throwsNotFound() {
        when(cache.get("ORD-NOPE")).thenReturn(null);
        when(historyDao.findLatestByOrderId("ORD-NOPE"))
                .thenReturn(DaoResult.notFound("missing"));

        assertThatThrownBy(() -> trackingService.getTracking("ORD-NOPE"))
                .isInstanceOf(TrackingService.OrderNotFoundException.class);
    }

    @Test
    void resolveCurrentStatus_cacheMiss_returnsNullWhenNoHistory() {
        when(cache.get("ORD-EMPTY")).thenReturn(null);
        when(historyDao.findLatestByOrderId("ORD-EMPTY"))
                .thenReturn(DaoResult.notFound("missing"));

        assertThat(trackingService.resolveCurrentStatus("ORD-EMPTY")).isNull();
    }
}
