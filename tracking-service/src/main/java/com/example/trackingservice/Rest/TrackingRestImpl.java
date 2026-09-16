package com.example.trackingservice.Rest;

import com.example.trackingservice.constant.TrackingConstants;
import com.example.trackingservice.dto.ApiErrorResponse;
import com.example.trackingservice.dto.TrackingResponse;
import com.example.trackingservice.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrackingRestImpl implements TrackingRest {

    private static final Logger log = LoggerFactory.getLogger(TrackingRestImpl.class);
    private final TrackingService trackingService;

    public TrackingRestImpl(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @Override
    public ResponseEntity<TrackingResponse> getTracking(String orderId) {
        log.info("GET /api/tracking/orders/{}", orderId);
        return ResponseEntity.ok(TrackingResponse.from(trackingService.getTracking(orderId)));
    }

    @ExceptionHandler(TrackingService.OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> onNotFound(TrackingService.OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(TrackingConstants.ERROR_NOT_TRACKED, ex.getMessage()));
    }
}
