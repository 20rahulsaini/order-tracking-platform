package com.example.trackingservice.Rest;

import com.example.trackingservice.model.OrderStatusHistory;
import com.example.trackingservice.service.TrackingService;
import com.example.trackingservice.service.TrackingService.TrackingSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer test for {@link TrackingRestImpl}.
 */
@WebMvcTest(TrackingRestImpl.class)
class TrackingRestImplTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackingService trackingService;

    @Test
    void getTracking_returnsCurrentStatusAndTimeline() throws Exception {
        OrderStatusHistory ev = new OrderStatusHistory(
                "ORD-ABCD", "PLACED", "ORDER_PLACED", "EVT-1",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:01Z")
        );
        TrackingSnapshot snapshot = new TrackingSnapshot(
                "ORD-ABCD",
                "PLACED",
                Instant.parse("2024-01-01T00:00:00Z"),
                List.of(ev)
        );
        when(trackingService.getTracking("ORD-ABCD")).thenReturn(snapshot);

        mockMvc.perform(get("/api/tracking/orders/ORD-ABCD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-ABCD"))
                .andExpect(jsonPath("$.currentStatus").value("PLACED"))
                .andExpect(jsonPath("$.timeline[0].status").value("PLACED"))
                .andExpect(jsonPath("$.timeline[0].eventType").value("ORDER_PLACED"))
                .andExpect(jsonPath("$.timeline[0].eventId").value("EVT-1"));
    }

    @Test
    void getTracking_returns404WhenUntracked() throws Exception {
        when(trackingService.getTracking(anyString()))
                .thenThrow(new TrackingService.OrderNotFoundException("ORD-MISSING"));

        mockMvc.perform(get("/api/tracking/orders/ORD-MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_TRACKED"));
    }
}
