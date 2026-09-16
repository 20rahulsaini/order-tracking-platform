package com.example.deliveryservice.service;

import com.example.deliveryservice.dto.DeliveryEventResponse;

public interface DeliveryEventPublisherService {
    DeliveryEventResponse ship(String orderId);
    DeliveryEventResponse outForDelivery(String orderId);
    DeliveryEventResponse deliver(String orderId);
}
