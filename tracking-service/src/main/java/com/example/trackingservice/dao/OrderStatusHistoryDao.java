package com.example.trackingservice.dao;

import com.example.trackingservice.model.OrderStatusHistory;
import java.util.List;

public interface OrderStatusHistoryDao {
    DaoResult<OrderStatusHistory> save(OrderStatusHistory history);

    DaoResult<OrderStatusHistory> findLatestByOrderId(String orderId);

    DaoResult<List<OrderStatusHistory>> findTimelineByOrderId(String orderId);
    
    DaoResult<Boolean> existsByEventId(String eventId);
}
