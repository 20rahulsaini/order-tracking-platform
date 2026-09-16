package com.example.deliveryservice.service;

import com.example.deliveryservice.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class DeliveryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventPublisher.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String topicName;

    public DeliveryEventPublisher(KafkaTemplate<String, OrderEvent> kafkaTemplate,
                                  @Value("${order.events.topic:order-events}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publish(String orderId, String eventType, String status) {
        String eventId = "EVT-" + UUID.randomUUID();
        OrderEvent event = new OrderEvent(
                eventId,
                orderId,
                eventType,
                status,
                Instant.now()
        );
        log.info("Publishing delivery event eventId={} orderId={} eventType={} status={}",
                eventId, orderId, eventType, status);
        kafkaTemplate.send(topicName, orderId, event);
        // Don't block - delivery simulation is fire-and-forget.
        log.info("Delivery event sent to Kafka topic={} key=orderId={} eventId={}",
                topicName, orderId, eventId);
    }
}
