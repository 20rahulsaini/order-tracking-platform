package com.example.trackingservice.dao;

import com.example.trackingservice.model.OrderStatusHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class OrderStatusHistoryDaoImpl implements OrderStatusHistoryDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public DaoResult<OrderStatusHistory> save(OrderStatusHistory history) {
        entityManager.persist(history);
        entityManager.flush();
        return DaoResult.success(history, "Order status history persisted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public DaoResult<OrderStatusHistory> findLatestByOrderId(String orderId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<OrderStatusHistory> query = cb.createQuery(OrderStatusHistory.class);
        Root<OrderStatusHistory> root = query.from(OrderStatusHistory.class);

        query.select(root)
                .where(cb.equal(root.get("orderId"), orderId))
                .orderBy(cb.desc(root.get("occurredAt")));

        List<OrderStatusHistory> result =
                entityManager.createQuery(query).setMaxResults(1).getResultList();

        return result.isEmpty()
                ? DaoResult.notFound("No tracking history exists for order: " + orderId)
                : DaoResult.found(result.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public DaoResult<List<OrderStatusHistory>> findTimelineByOrderId(String orderId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<OrderStatusHistory> query = cb.createQuery(OrderStatusHistory.class);
        Root<OrderStatusHistory> root = query.from(OrderStatusHistory.class);

        query.select(root)
                .where(cb.equal(root.get("orderId"), orderId))
                .orderBy(cb.asc(root.get("occurredAt")));

        List<OrderStatusHistory> result = entityManager.createQuery(query).getResultList();
        return DaoResult.success(result, "Tracking timeline retrieved successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public DaoResult<Boolean> existsByEventId(String eventId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<OrderStatusHistory> root = query.from(OrderStatusHistory.class);

        query.select(cb.count(root))
                .where(cb.equal(root.get("eventId"), eventId));

        Long count = entityManager.createQuery(query).getSingleResult();
        return DaoResult.success(count > 0, "Event existence checked successfully");
    }
}
