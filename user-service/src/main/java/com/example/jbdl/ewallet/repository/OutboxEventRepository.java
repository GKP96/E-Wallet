package com.example.jbdl.ewallet.repository;

import com.example.jbdl.ewallet.entity.OutboxEvent;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface OutboxEventRepository extends CrudRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByPublishedFalseAndRetryCountLessThan(int retryCount);
}
