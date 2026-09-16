package com.example.deliveryservice.Rest;

import com.example.deliveryservice.config.CommonApiResponses;
import com.example.deliveryservice.dto.DeliveryEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Delivery", description = "Simulate order delivery lifecycle events")
@RequestMapping("/api/delivery")
public interface DeliveryRest {

    @Operation(
            summary = "Mark an order as SHIPPED",
            description = "Publishes an ORDER_SHIPPED event to Kafka."
    )
    @CommonApiResponses
    @PostMapping("/{orderId}/ship")
    ResponseEntity<DeliveryEventResponse> ship(
            @Parameter(description = "Business identifier", required = true, example = "ORD-ABCD1234")
            @PathVariable String orderId);

    @Operation(
            summary = "Mark an order as OUT_FOR_DELIVERY",
            description = "Publishes an ORDER_OUT_FOR_DELIVERY event to Kafka."
    )
    @CommonApiResponses
    @PostMapping("/{orderId}/out-for-delivery")
    ResponseEntity<DeliveryEventResponse> outForDelivery(
            @Parameter(description = "Business identifier", required = true, example = "ORD-ABCD1234")
            @PathVariable String orderId);

    @Operation(
            summary = "Mark an order as DELIVERED",
            description = "Publishes an ORDER_DELIVERED event to Kafka."
    )
    @CommonApiResponses
    @PostMapping("/{orderId}/deliver")
    ResponseEntity<DeliveryEventResponse> deliver(
            @Parameter(description = "Business identifier", required = true, example = "ORD-ABCD1234")
            @PathVariable String orderId);
}
