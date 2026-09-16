package com.example.trackingservice.dao;

import com.example.trackingservice.model.ProcessedEvent;

public interface ProcessedEventDao {
    DaoResult<ProcessedEvent> save(ProcessedEvent event);
    
    DaoResult<ProcessedEvent> findByEventId(String eventId);
}
