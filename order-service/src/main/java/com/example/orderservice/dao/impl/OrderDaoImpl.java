package com.example.orderservice.dao;

import com.example.orderservice.model.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderDaoImpl implements OrderDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public DaoResult<Order> save(Order order) {
        entityManager.persist(order);
        entityManager.flush();
        return DaoResult.success(order, "Order persisted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public Order findByOrderId(String orderId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> query = cb.createQuery(Order.class);
        Root<Order> root = query.from(Order.class);

        query.select(root)
                .where(cb.equal(root.get("orderId"), orderId));

        TypedQuery<Order> typedQuery = entityManager.createQuery(query);

          return typedQuery.getResultStream()
            .findFirst()
            .orElse(null);
    }


    @Override
    @Transactional(readOnly = true)
    public List<Order> findAllOrders() {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Order> query = cb.createQuery(Order.class);
    Root<Order> root = query.from(Order.class);
    query.select(root);
        TypedQuery<Order> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }
}
