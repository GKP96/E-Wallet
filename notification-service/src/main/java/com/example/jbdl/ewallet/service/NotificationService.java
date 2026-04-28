package com.example.jbdl.ewallet.service;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jbdl.ewallet.entity.NotificationOutboxEvent;
import com.example.jbdl.ewallet.entity.ProcessedWalletTransaction;
import com.example.jbdl.ewallet.repository.NotificationOutboxRepository;
import com.example.jbdl.ewallet.repository.ProccessedWalletTransactionRepository;

@Service
public class NotificationService {

    @Autowired
    SimpleMailMessage simpleMailMessage;

    @Autowired
    JavaMailSender javaMailSender;

    @Autowired
    private NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    private ProccessedWalletTransactionRepository processedWalletTransactionRepository;

    private static final String TXN_COMPLETE_TOPIC = "txn_complete";

    @KafkaListener(topics = { TXN_COMPLETE_TOPIC }, groupId = "jbdl61_grp")
    @Transactional
    public void sendNotif(String msg) throws Exception {
        JSONObject jsonObject = (JSONObject) new JSONParser().parse(msg);
        String txnId = (String) jsonObject.get("txnId");
        String status = (String) jsonObject.get("status");
        String senderEmail = (String) jsonObject.get("sender");
        String receiverEmail = (String) jsonObject.get("receiver");
        Double amount = (Double) jsonObject.get("amount");

        ProcessedWalletTransaction existing = processedWalletTransactionRepository.findById(txnId).orElse(null);
        if (existing != null) {
            return;
        }

        ProcessedWalletTransaction processed = new ProcessedWalletTransaction();
        processed.setTransId(txnId);
        processedWalletTransactionRepository.save(processed);

        // Save outbox event for sender
        NotificationOutboxEvent senderEvent = new NotificationOutboxEvent();
        senderEvent.setTxnId(txnId);
        senderEvent.setEventType("EMAIL");
        senderEvent.setPayload(String.format("to:%s|subject:%s|body:%s", senderEmail, "Payment Notification",
                "Hi, your txn with id " + txnId + " got " + status));
        senderEvent.setSent(false);
        notificationOutboxRepository.save(senderEvent);

        // Save outbox event for receiver if successful
        if ("SUCCESSFUL".equals(status)) {
            NotificationOutboxEvent receiverEvent = new NotificationOutboxEvent();
            receiverEvent.setTxnId(txnId);
            receiverEvent.setEventType("EMAIL");
            receiverEvent.setPayload(String.format("to:%s|subject:%s|body:%s", receiverEmail, "Payment Notification",
                    "Hi, you got amount " + amount + " from user " + senderEmail + " in your e-wallet"));
            receiverEvent.setSent(false);
            notificationOutboxRepository.save(receiverEvent);
        }
    }

}
