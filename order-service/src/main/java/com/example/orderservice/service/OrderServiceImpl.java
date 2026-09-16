package com.example.orderservice.service;

import com.example.orderservice.constant.OrderConstants;
import com.example.orderservice.dao.DaoResult;
import com.example.orderservice.dao.OrderDao;
import com.example.orderservice.dto.OrderEvent;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.kafka.OrderEventProducer;
import com.example.orderservice.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderDao orderDao;
    private final OrderEventProducer eventProducer;

    public OrderServiceImpl(OrderDao orderDao, OrderEventProducer eventProducer) {
        this.orderDao = orderDao;
        this.eventProducer = eventProducer;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer={} product={} quantity={} amount={}",
                request.getCustomerName(), request.getProduct(),
                request.getQuantity(), request.getTotalAmount());

        Instant now = Instant.now();
        String orderId = generateOrderId();
        String eventId = OrderConstants.EVENT_ID_PREFIX + UUID.randomUUID();

        Order order = new Order();
        order.setOrderId(orderId);
        order.setCustomerName(request.getCustomerName());
        order.setProduct(request.getProduct());
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(request.getTotalAmount());
        order.setStatus(OrderConstants.STATUS_PLACED);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        DaoResult<Order> daoResult = orderDao.save(order);
        Order saved = daoResult.getData();

        OrderEvent event = new OrderEvent(eventId, orderId, OrderConstants.EVENT_ORDER_PLACED,
                OrderConstants.STATUS_PLACED, now
        );

        try {
             eventProducer.publish(event);
                log.info("Order lifecycle event published orderId={} eventId={}",
                 orderId, eventId);
        } catch (Exception e) {
               log.error("Order created but event publishing failed orderId={} eventId={}",
                orderId, eventId, e);
        }
        log.info("Order created and lifecycle event published orderId={} eventId={}",
                orderId, eventId);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        log.info("Fetching order orderId={}", orderId);
        DaoResult<Order> daoResult = orderDao.findByOrderId(orderId);

        if (!daoResult.isFound()) {
            log.warn("Order not found orderId={}", orderId);
            throw new OrderNotFoundException(orderId);
        }

        return toResponse(daoResult.getData());
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setCustomerName(order.getCustomerName());
        response.setProduct(order.getProduct());
        response.setQuantity(order.getQuantity());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }

    private String generateOrderId() {
        return OrderConstants.ORDER_ID_PREFIX +
                UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
