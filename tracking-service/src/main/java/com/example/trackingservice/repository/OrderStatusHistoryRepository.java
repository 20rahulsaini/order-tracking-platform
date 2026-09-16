package com.example.trackingservice.repository;

import com.example.trackingservice.model.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderIdOrderByOccurredAtAsc(String orderId);

    Optional<OrderStatusHistory> findTopByOrderIdOrderByOccurredAtDesc(String orderId);

    boolean existsByEventId(String eventId);
}
