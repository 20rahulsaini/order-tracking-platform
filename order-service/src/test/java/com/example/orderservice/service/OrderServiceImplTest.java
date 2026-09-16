package com.example.orderservice.service;

import com.example.orderservice.dao.DaoResult;
import com.example.orderservice.dao.OrderDao;
import com.example.orderservice.dto.OrderEvent;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.kafka.OrderEventProducer;
import com.example.orderservice.model.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderDao orderDao;
    @Mock private OrderEventProducer eventProducer;
    @InjectMocks private OrderServiceImpl orderService;

    @Test
    void createOrder_persistsOrderAndPublishesEvent() {
        when(orderDao.save(any(Order.class))).thenAnswer(inv ->
                DaoResult.success(inv.getArgument(0), "saved"));

        OrderRequest request = new OrderRequest();
        request.setCustomerName("Alice");
        request.setProduct("Keyboard");
        request.setQuantity(2);
        request.setTotalAmount(new BigDecimal("199.98"));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.getOrderId()).startsWith("ORD-");
        assertThat(response.getStatus()).isEqualTo("PLACED");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderDao).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("PLACED");

        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("ORDER_PLACED");
    }

    @Test
    void getOrder_returnsOrderWhenFound() {
        Order order = new Order();
        order.setOrderId("ORD-EXISTING");
        order.setCustomerName("Bob");
        order.setProduct("Pen");
        order.setQuantity(1);
        order.setTotalAmount(new BigDecimal("1.99"));
        order.setStatus("PLACED");

        when(orderDao.findByOrderId("ORD-EXISTING")).thenReturn(order);

        assertThat(orderService.getOrder("ORD-EXISTING").getOrderId())
                .isEqualTo("ORD-EXISTING");
    }

    @Test
    void getOrder_throwsWhenNotFound() {
        when(orderDao.findByOrderId("ORD-MISSING"))
                .thenReturn(null);

        assertThatThrownBy(() -> orderService.getOrder("ORD-MISSING"))
                .isInstanceOf(OrderService.OrderNotFoundException.class);
    }
}
