package com.example.orderservice.kafka;

import com.example.orderservice.dto.OrderEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrderEventProducer}.
 *
 * Covers:
 *   - Happy path: producer delegates to kafkaTemplate.send with the
 *     correct topic, key (=orderId), and payload. The synchronous
 *     future.join() completes cleanly when the send resolves.
 *   - Failure path: if kafkaTemplate.send fails (broker unreachable),
 *     publish re-throws so the REST layer can fail-fast.
 */
@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private OrderEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new OrderEventProducer(kafkaTemplate, "order-events");
    }

    @Test
    void publish_sendsWithOrderIdAsKey_andTopicName() {
        OrderEvent event = new OrderEvent(
                "EVT-1", "ORD-1", "ORDER_PLACED", "PLACED", Instant.now());
        // Spring Kafka 3.x returns CompletableFuture
        when(kafkaTemplate.send(eq("order-events"), eq("ORD-1"), eq(event)))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.publish(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderEvent> payloadCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo("order-events");
        assertThat(keyCaptor.getValue()).isEqualTo("ORD-1");
        assertThat(payloadCaptor.getValue()).isSameAs(event);
    }

    @Test
    void publish_propagatesSendFailure() {
        OrderEvent event = new OrderEvent(
                "EVT-2", "ORD-2", "ORDER_PLACED", "PLACED", Instant.now());
        CompletableFuture<SendResult<String, OrderEvent>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unreachable"));
        when(kafkaTemplate.send(anyString(), anyString(), eq(event)))
                .thenReturn(failed);

        assertThatThrownBy(() -> producer.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("broker unreachable");
    }
}
