package com.example.jbdl.ewallet.repository;

import org.springframework.data.repository.CrudRepository;

import com.example.jbdl.ewallet.entity.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository extends CrudRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByPublishedFalse();
}
