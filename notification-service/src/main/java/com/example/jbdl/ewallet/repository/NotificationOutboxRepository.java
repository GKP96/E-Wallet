package com.example.jbdl.ewallet.repository;

import com.example.jbdl.ewallet.entity.NotificationOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEvent, Long> {
    List<NotificationOutboxEvent> findBySentFalseAndRetryCountLessThan(int retryCount);
}
