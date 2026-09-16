package com.example.orderservice.kafka;

import com.example.orderservice.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;


@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String topicName;

    public OrderEventProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate,
                              @Value("${order.events.topic:order-events}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publish(OrderEvent event) {
        log.info("Publishing event eventId={} orderId={} eventType={} status={} to topic {} with key orderId={}",
                event.getEventId(),
                event.getOrderId(),
                event.getEventType(),
                event.getStatus(),
                topicName,
                event.getOrderId());
        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(topicName, event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event eventId={} orderId={} reason={}",
                        event.getEventId(), event.getOrderId(), ex.getMessage(), ex);
            } else if (result != null && result.getRecordMetadata() != null) {
                org.apache.kafka.clients.producer.RecordMetadata meta = result.getRecordMetadata();
                log.info("Event acknowledged eventId={} orderId={} topic={} partition={} offset={}",
                        event.getEventId(), event.getOrderId(), topicName, meta.partition(), meta.offset());
            } else {
                log.info("Event acknowledged eventId={} orderId={} (no metadata available)",
                        event.getEventId(), event.getOrderId());
            }
        });

        future.join();
        log.info("Kafka send completed (synchronous) eventId={} orderId={}",
                event.getEventId(), event.getOrderId());
    }

    // Convenience accessor for tests
    public String topicName() {
        return topicName;
    }

    
    public static final class RecordMetadata {
        private final long offset;
        private final int partition;

        public RecordMetadata(long offset, int partition) {
            this.offset = offset;
            this.partition = partition;
        }

        public long offset() { return offset; }
        public int partition() { return partition; }
    }
}
