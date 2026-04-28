package com.example.jbdl.ewallet.service;

import com.example.jbdl.ewallet.entity.OutboxEvent;
import com.example.jbdl.ewallet.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserOutboxPublisher {
    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String USER_CREATE_TOPIC = "user_create";

    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findByPublishedFalseAndRetryCountLessThan(MAX_RETRIES);
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(USER_CREATE_TOPIC, event.payload);
                event.published = true;
                outboxRepository.save(event);
            } catch (Exception e) {
                event.retryCount++;
                outboxRepository.save(event);
            }
        }
    }
}
