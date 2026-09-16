package com.example.trackingservice.kafka;

import com.example.trackingservice.dto.OrderEvent;
import com.example.trackingservice.service.TrackingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final TrackingService trackingService;

    public OrderEventConsumer(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @KafkaListener(
            topics = "${order.events.topic:order-events}",
            groupId = "${tracking.kafka.consumer-group:tracking-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRecord(ConsumerRecord<String, OrderEvent> record) {
        OrderEvent event = record.value();
        log.info("Consumed Kafka record topic={} partition={} offset={} key={} eventId={} orderId={} status={}",
                record.topic(), record.partition(), record.offset(),
                record.key(),
                event.getEventId(),
                event.getOrderId(),
                event.getStatus());
        try {
            boolean applied = trackingService.applyEvent(event);
            if (applied) {
                log.info("Consumer: event {} applied for orderId={}", event.getEventId(), event.getOrderId());
            } else {
                log.info("Consumer: event {} skipped (duplicate) for orderId={}",
                        event.getEventId(), event.getOrderId());
            }
        } catch (com.example.trackingservice.service.InvalidStatusTransitionException e) {
            log.error("Invalid status transition for order {}: {} -> {}. Will route to DLQ.",
                    e.getOrderId(), e.getCurrent(), e.getTarget());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while processing event eventId={} orderId={}. Will retry or DLQ.",
                    event.getEventId(), event.getOrderId(), e);
            throw e;
        }
    }
}
