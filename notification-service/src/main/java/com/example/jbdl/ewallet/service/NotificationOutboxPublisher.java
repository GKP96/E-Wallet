package com.example.jbdl.ewallet.service;

import com.example.jbdl.ewallet.entity.NotificationOutboxEvent;
import com.example.jbdl.ewallet.repository.NotificationOutboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationOutboxPublisher {
    @Autowired
    private NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    private SimpleMailMessage simpleMailMessage;

    @Autowired
    private JavaMailSender javaMailSender;

    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<NotificationOutboxEvent> events = notificationOutboxRepository.findBySentFalseAndRetryCountLessThan(MAX_RETRIES);
        for (NotificationOutboxEvent event : events) {
            if ("EMAIL".equals(event.getEventType())) {
                try {
                    String[] parts = event.getPayload().split("\\|", 3);
                    String to = parts[0].substring(3);
                    String subject = parts[1].substring(8);
                    String body = parts[2].substring(5);
                    if (to == null || to.equals("unknown") || to.trim().isEmpty()) {
                        System.out.println("Skipping email send for unknown/invalid address: " + to);
                        event.setSent(true);
                        notificationOutboxRepository.save(event);
                        continue;
                    }
                    
                    simpleMailMessage.setTo(to);
                    simpleMailMessage.setSubject(subject);
                    simpleMailMessage.setText(body);
                    simpleMailMessage.setFrom("learningwithai2000@gmail.com");
                    System.out.println("Attempting to send email to: " + to);
                    javaMailSender.send(simpleMailMessage);
                    event.setSent(true);
                    System.out.println("Email sent successfully to: " + to);
                    notificationOutboxRepository.save(event);
                } catch (Exception e) {
                    System.out.println("Failed to send email to " + event.getPayload() + ". Error: " + e.getMessage());
                    e.printStackTrace();
                    event.setRetryCount(event.getRetryCount() + 1);
                    notificationOutboxRepository.save(event);
                }
            }
        }
    }
}
