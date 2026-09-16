package com.example.deliveryservice.service;

import com.example.deliveryservice.constant.DeliveryConstants;
import com.example.deliveryservice.dto.DeliveryEventResponse;
import com.example.deliveryservice.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class DeliveryEventPublisherServiceImpl implements DeliveryEventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventPublisherServiceImpl.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String topicName;

    public DeliveryEventPublisherServiceImpl(
            KafkaTemplate<String, OrderEvent> kafkaTemplate,
            @Value("${order.events.topic:order-events}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    @Override
    public DeliveryEventResponse ship(String orderId) {
        return publish(orderId, DeliveryConstants.EVENT_ORDER_SHIPPED,
                DeliveryConstants.STATUS_SHIPPED);
    }

    @Override
    public DeliveryEventResponse outForDelivery(String orderId) {
        return publish(orderId, DeliveryConstants.EVENT_ORDER_OUT_FOR_DELIVERY,
                DeliveryConstants.STATUS_OUT_FOR_DELIVERY);
    }

    @Override
    public DeliveryEventResponse deliver(String orderId) {
        return publish(orderId, DeliveryConstants.EVENT_ORDER_DELIVERED,
                DeliveryConstants.STATUS_DELIVERED);
    }

    private DeliveryEventResponse publish(String orderId, String eventType, String status) {
        String eventId = DeliveryConstants.EVENT_ID_PREFIX + UUID.randomUUID();
        OrderEvent event = new OrderEvent(eventId, orderId, eventType, status, Instant.now());

        log.info("Publishing delivery event eventId={} orderId={} eventType={} status={}",
                eventId, orderId, eventType, status);

        kafkaTemplate.send(topicName, orderId, event);

        return new DeliveryEventResponse(orderId, status, eventType, true);
    }
}
