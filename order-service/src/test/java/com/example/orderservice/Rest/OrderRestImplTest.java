package com.example.orderservice.Rest;

import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(OrderRestImpl.class)
class OrderRestImplTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void postOrders_createsAndReturns201() throws Exception {
        OrderResponse stub = new OrderResponse();
        stub.setOrderId("ORD-ABCD");
        stub.setCustomerName("Alice");
        stub.setProduct("Mech Keyboard");
        stub.setQuantity(2);
        stub.setTotalAmount(new BigDecimal("199.98"));
        stub.setStatus("PLACED");
        stub.setCreatedAt(Instant.now());
        stub.setUpdatedAt(Instant.now());
        when(orderService.createOrder(any())).thenReturn(stub);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Alice",
                                  "product": "Mech Keyboard",
                                  "quantity": 2,
                                  "totalAmount": 199.98
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("ORD-ABCD"))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.customerName").value("Alice"));
    }

    @Test
    void postOrders_validatesPayload_andReturns400WhenMissingFields() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "customerName": "Alice" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_returns404WhenMissing() throws Exception {
        when(orderService.getOrder("ORD-MISSING"))
                .thenThrow(new OrderService.OrderNotFoundException("ORD-MISSING"));

        mockMvc.perform(get("/api/orders/ORD-MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
