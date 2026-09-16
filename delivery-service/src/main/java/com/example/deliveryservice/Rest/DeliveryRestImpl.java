package com.example.deliveryservice.Rest;

import com.example.deliveryservice.dto.DeliveryEventResponse;
import com.example.deliveryservice.service.DeliveryEventPublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeliveryRestImpl implements DeliveryRest {

    private static final Logger log = LoggerFactory.getLogger(DeliveryRestImpl.class);
    private final DeliveryEventPublisherService deliveryEventPublisherService;

    public DeliveryRestImpl(DeliveryEventPublisherService deliveryEventPublisherService) {
        this.deliveryEventPublisherService = deliveryEventPublisherService;
    }

    @Override
    public ResponseEntity<DeliveryEventResponse> ship(String orderId) {
        log.info("POST /api/delivery/{}/ship", orderId);
        return ResponseEntity.ok(deliveryEventPublisherService.ship(orderId));
    }

    @Override
    public ResponseEntity<DeliveryEventResponse> outForDelivery(String orderId) {
        log.info("POST /api/delivery/{}/out-for-delivery", orderId);
        return ResponseEntity.ok(deliveryEventPublisherService.outForDelivery(orderId));
    }

    @Override
    public ResponseEntity<DeliveryEventResponse> deliver(String orderId) {
        log.info("POST /api/delivery/{}/deliver", orderId);
        return ResponseEntity.ok(deliveryEventPublisherService.deliver(orderId));
    }
}
