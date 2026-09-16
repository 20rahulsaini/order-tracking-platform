package com.example.trackingservice.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link OrderStateCache} wrapper.
 *
 * Covers:
 *   - Cache HIT: returns deserialized state from Redis.
 *   - Cache MISS: returns null (caller falls back to MySQL).
 *   - Cache write: put serializes the state.
 *   - Cache degrade: if Redis throws, get returns null (no exception bubbled).
 */
@ExtendWith(MockitoExtension.class)
class OrderStateCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private OrderStateCache cache;

    @BeforeEach
    void setUp() {
        cache = new OrderStateCache(redisTemplate, new ObjectMapper(), 3600L);
    }

    @Test
    void get_returnsNullOnMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("order:ORD-1")).thenReturn(null);

        OrderStateCache.CachedOrderState result = cache.get("ORD-1");

        assertThat(result).isNull();
    }

    @Test
    void get_returnsStateOnHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        String json = "{\"orderId\":\"ORD-1\",\"status\":\"SHIPPED\","
                + "\"lastEventType\":\"ORDER_SHIPPED\","
                + "\"lastEventId\":\"EVT-9\","
                + "\"lastUpdated\":\"2024-01-01T00:00:00Z\"}";
        when(valueOps.get("order:ORD-1")).thenReturn(json);

        OrderStateCache.CachedOrderState result = cache.get("ORD-1");

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORD-1");
        assertThat(result.getStatus()).isEqualTo("SHIPPED");
        assertThat(result.getLastEventId()).isEqualTo("EVT-9");
    }

    @Test
    void put_writesSerializedJson() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        OrderStateCache.CachedOrderState state =
                new OrderStateCache.CachedOrderState(
                        "ORD-1", "SHIPPED", "ORDER_SHIPPED", "EVT-9",
                        Instant.parse("2024-01-01T00:00:00Z")
                );
        cache.put(state);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(keyCaptor.capture(), valueCaptor.capture(), anyLong());
        assertThat(keyCaptor.getValue()).isEqualTo("order:ORD-1");
        assertThat(valueCaptor.getValue()).contains("\"orderId\":\"ORD-1\"");
        assertThat(valueCaptor.getValue()).contains("\"status\":\"SHIPPED\"");
    }

    @Test
    void get_degradesGracefullyOnRedisException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("connection lost"));

        // Should not propagate; should return null so the caller falls back to MySQL
        OrderStateCache.CachedOrderState result = cache.get("ORD-1");
        assertThat(result).isNull();
    }
}
