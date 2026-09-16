package com.example.deliveryservice.service;

import com.example.deliveryservice.dto.OrderEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryEventPublisherServiceImplTest {

    @Mock private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private DeliveryEventPublisherServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeliveryEventPublisherServiceImpl(kafkaTemplate, "order-events");
    }

    @Test
    void ship_publishesCorrectEvent() {
        when(kafkaTemplate.send(anyString(), anyString(), any(OrderEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        var response = service.ship("ORD-1");

        assertThat(response.getStatus()).isEqualTo("SHIPPED");
        assertThat(response.getEventType()).isEqualTo("ORDER_SHIPPED");

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(kafkaTemplate).send(eq("order-events"), eq("ORD-1"), captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo("ORD-1");
        assertThat(captor.getValue().getEventId()).startsWith("EVT-");
    }
}
