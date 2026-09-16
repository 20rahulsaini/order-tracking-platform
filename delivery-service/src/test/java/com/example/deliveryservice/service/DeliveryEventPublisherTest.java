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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DeliveryEventPublisher}.
 *
 * <p>Verifies the publisher sends the right topic + key + payload shape.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryEventPublisherTest {

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private DeliveryEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new DeliveryEventPublisher(kafkaTemplate, "order-events");
    }

    @Test
    void publish_emitsCorrectShape() {
        when(kafkaTemplate.send(anyString(), anyString(), any(OrderEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish("ORD-1", "ORDER_SHIPPED", "SHIPPED");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderEvent> payloadCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("order-events");
        assertThat(keyCaptor.getValue()).isEqualTo("ORD-1"); // orderId as key
        assertThat(payloadCaptor.getValue().getOrderId()).isEqualTo("ORD-1");
        assertThat(payloadCaptor.getValue().getEventType()).isEqualTo("ORDER_SHIPPED");
        assertThat(payloadCaptor.getValue().getStatus()).isEqualTo("SHIPPED");
        assertThat(payloadCaptor.getValue().getEventId()).startsWith("EVT-");
    }
}
