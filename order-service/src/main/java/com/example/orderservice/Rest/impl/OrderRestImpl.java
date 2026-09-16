package com.example.orderservice.Rest;

import com.example.orderservice.constant.OrderConstants;
import com.example.orderservice.dto.ApiErrorResponse;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderRestImpl implements OrderRest {

    private static final Logger log = LoggerFactory.getLogger(OrderRestImpl.class);
    private final OrderService orderService;

    public OrderRestImpl(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public ResponseEntity<OrderResponse> createOrder(OrderRequest request) {
        log.info("POST /api/orders customer={} product={}", request.getCustomerName(), request.getProduct());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<OrderResponse> getOrder(String orderId) {
        log.info("GET /api/orders/{}", orderId);
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @ExceptionHandler(OrderService.OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> onNotFound(OrderService.OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(OrderConstants.ERROR_NOT_FOUND, ex.getMessage()));
    }
}
