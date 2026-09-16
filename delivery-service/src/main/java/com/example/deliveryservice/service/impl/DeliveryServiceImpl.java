package com.example.deliveryservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class DeliveryServiceImpl implements DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryServiceImpl.class);

    private final DeliveryEventPublisher publisher;

    public DeliveryServiceImpl(DeliveryEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void ship(String orderId) {
        log.info("Service: ship requested orderId={}", orderId);
        publisher.publish(orderId, "ORDER_SHIPPED", "SHIPPED");
        log.info("Service: ship event emitted orderId={}", orderId);
    }

    @Override
    public void outForDelivery(String orderId) {
        log.info("Service: out-for-delivery requested orderId={}", orderId);
        publisher.publish(orderId, "ORDER_OUT_FOR_DELIVERY", "OUT_FOR_DELIVERY");
        log.info("Service: out-for-delivery event emitted orderId={}", orderId);
    }

    @Override
    public void deliver(String orderId) {
        log.info("Service: deliver requested orderId={}", orderId);
        publisher.publish(orderId, "ORDER_DELIVERED", "DELIVERED");
        log.info("Service: deliver event emitted orderId={}", orderId);
    }
}
