package com.example.trackingservice.dao;

import com.example.trackingservice.model.ProcessedEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ProcessedEventDaoImpl implements ProcessedEventDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public DaoResult<ProcessedEvent> save(ProcessedEvent event) {
        entityManager.persist(event);
        entityManager.flush();
        return DaoResult.success(event, "Processed event persisted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public DaoResult<ProcessedEvent> findByEventId(String eventId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProcessedEvent> query = cb.createQuery(ProcessedEvent.class);
        Root<ProcessedEvent> root = query.from(ProcessedEvent.class);

        query.select(root).where(cb.equal(root.get("eventId"), eventId));

        List<ProcessedEvent> result = entityManager.createQuery(query).getResultList();
        return result.isEmpty()
                ? DaoResult.notFound("Processed event does not exist: " + eventId)
                : DaoResult.found(result.get(0));
    }
}
