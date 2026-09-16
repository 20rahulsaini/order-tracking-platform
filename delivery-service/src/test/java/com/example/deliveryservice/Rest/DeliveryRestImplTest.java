package com.example.deliveryservice.Rest;

import com.example.deliveryservice.dto.DeliveryEventResponse;
import com.example.deliveryservice.service.DeliveryEventPublisherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeliveryRestImpl.class)
class DeliveryRestImplTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DeliveryEventPublisherService deliveryService;

    @Test
    void ship_returnsPublishedResponse() throws Exception {
        when(deliveryService.ship("ORD-1"))
                .thenReturn(new DeliveryEventResponse("ORD-1", "SHIPPED", "ORDER_SHIPPED", true));

        mockMvc.perform(post("/api/delivery/ORD-1/ship"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-1"))
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.published").value(true));

        verify(deliveryService).ship(eq("ORD-1"));
    }

    @Test
    void outForDelivery_returnsPublishedResponse() throws Exception {
        when(deliveryService.outForDelivery("ORD-2"))
                .thenReturn(new DeliveryEventResponse("ORD-2", "OUT_FOR_DELIVERY", "ORDER_OUT_FOR_DELIVERY", true));

        mockMvc.perform(post("/api/delivery/ORD-2/out-for-delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_FOR_DELIVERY"));
    }

    @Test
    void deliver_returnsPublishedResponse() throws Exception {
        when(deliveryService.deliver("ORD-3"))
                .thenReturn(new DeliveryEventResponse("ORD-3", "DELIVERED", "ORDER_DELIVERED", true));

        mockMvc.perform(post("/api/delivery/ORD-3/deliver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }
}
