package com.example.trackingservice.Rest;

import com.example.trackingservice.config.CommonApiResponses;
import com.example.trackingservice.dto.TrackingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tracking", description = "Order tracking, status timeline, and cache-aside reads")
@RequestMapping("/api/tracking/orders")
public interface TrackingRest {

    @Operation(
            summary = "Get the current tracking snapshot for an order",
            description = "Returns current status, last-updated timestamp, and the full status timeline."
    )
    @CommonApiResponses
    @GetMapping("/{orderId}")
    ResponseEntity<TrackingResponse> getTracking(
            @Parameter(description = "Business identifier", required = true, example = "ORD-ABCD1234")
            @PathVariable String orderId);
}
