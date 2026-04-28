package com.example.jbdl.ewallet.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.jbdl.ewallet.entity.TransactionOutboxEvent;
import com.example.jbdl.ewallet.repository.TransactionOutboxEventRepository;

@Service
public class TransactionOutboxPublisher {

    @Autowired
    private TransactionOutboxEventRepository outboxRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String WALLET_UPDATE_TOPIC = "wallet_update";
    private static final String TXN_CREATE_TOPIC = "txn_create";

    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<TransactionOutboxEvent> events = outboxRepository.findBySentFalseAndRetryCountLessThan(MAX_RETRIES);

        for (TransactionOutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getPayload()).get();
                event.setSent(true);
                outboxRepository.save(event);
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                outboxRepository.save(event);
            }
        }
    }
}
