package com.example.jbdl.ewallet.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.jbdl.ewallet.entity.WalletOutboxEvent;
import com.example.jbdl.ewallet.repository.WalletOutboxRepository;

@Service
public class WalletOutboxPublisher {

    @Autowired
    private WalletOutboxRepository outboxRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String WALLET_UPDATE_TOPIC = "wallet_update";

    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<WalletOutboxEvent> events = outboxRepository.findBySentFalseAndRetryCountLessThan(MAX_RETRIES);

        for (WalletOutboxEvent event : events) {
            try {
                kafkaTemplate.send(WALLET_UPDATE_TOPIC, event.getPayload()).get();
                event.setSent(true);
                outboxRepository.save(event);
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                outboxRepository.save(event);
            }
        }
    }
}
