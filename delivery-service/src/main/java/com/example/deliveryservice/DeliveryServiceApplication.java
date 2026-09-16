package com.example.deliveryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Delivery Service - tiny simulation-only service.
 *
 * Goal: drive orders through the SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED
 * portion of the lifecycle by emitting Kafka events. There is no database
 * here on purpose - we want to demonstrate that events from a separate
 * producer flow through the same Kafka pipeline and land in the same
 * Tracking Service status history.
 */
@SpringBootApplication
public class DeliveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
