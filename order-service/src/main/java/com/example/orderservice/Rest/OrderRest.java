package com.example.orderservice.Rest;

import com.example.orderservice.config.CommonApiResponses;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Orders", description = "Create and fetch customer orders")
@RequestMapping("/api/orders")
public interface OrderRest {

    @Operation(
            summary = "Create a new order",
            description = "Persists an order with status PLACED and publishes the ORDER_PLACED lifecycle event."
    )
    @CommonApiResponses
    @PostMapping
    ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request);

    @Operation(
            summary = "Get an order by ID",
            description = "Returns the persisted order for the supplied business identifier."
    )
    @CommonApiResponses
    @GetMapping("/{orderId}")
    ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Business identifier", required = true, example = "ORD-ABCD1234")
            @PathVariable String orderId
    );
}
