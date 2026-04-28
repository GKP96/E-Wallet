package com.example.jbdl.ewallet.service;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.jbdl.ewallet.constants.TransactionStatus;
import com.example.jbdl.ewallet.entity.Transaction;
import com.example.jbdl.ewallet.entity.TransactionOutboxEvent;
import com.example.jbdl.ewallet.repository.TransactionOutboxEventRepository;
import com.example.jbdl.ewallet.repository.TransactionRepository;

import java.util.UUID;

@Service
public class TransactionService {

    private static final String TXN_CREATE_TOPIC = "txn_create";
    private static final String TXN_COMPLETE_TOPIC = "txn_complete";
    private static final String WALLET_UPDATE_TOPIC = "wallet_update";

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private UserClient userClient;

    @Autowired
    private TransactionOutboxEventRepository transactionOutboxEventRepository;

    @Transactional
    public String createTxn(Transaction transaction) {
        Transaction transaction2 = transactionRepository.findByIdempotencyKey(transaction.getIdempotencyKey());
        if (transaction2 != null) {
            // Return the original txnId for idempotency
            return transaction2.getTxnId();
        }
        transaction.setTxnId(UUID.randomUUID().toString());
        transaction.setTransactionStatus(TransactionStatus.PENDING);
        try {
            transactionRepository.save(transaction);
        } catch (Exception e) {
            // Handle race condition: if unique constraint fails, fetch and return existing
            // txn
            Transaction existing = transactionRepository.findByIdempotencyKey(transaction.getIdempotencyKey());
            if (existing != null) {
                return existing.getTxnId();
            }
            throw e;
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sender", transaction.getSenderUserId());
        jsonObject.put("receiver", transaction.getReceiverUserId());
        jsonObject.put("amount", transaction.getAmount());
        jsonObject.put("txnId", transaction.getTxnId());

        // kafkaTemplate.send(TXN_CREATE_TOPIC, jsonObject.toJSONString());
        TransactionOutboxEvent transactionOutboxEvent = new TransactionOutboxEvent();
        transactionOutboxEvent.setTxnId(transaction.getTxnId());
        transactionOutboxEvent.setEventType(TXN_CREATE_TOPIC);
        transactionOutboxEvent.setSent(false);
        transactionOutboxEvent.setPayload(jsonObject.toJSONString());

        transactionOutboxEventRepository.save(transactionOutboxEvent);

        return transaction.getTxnId();
    }

    @Transactional
    @KafkaListener(topics = { WALLET_UPDATE_TOPIC }, groupId = "jbdl61_grp")
    public void updateTxn(String msg) throws Exception {

        JSONObject jsonObject = (JSONObject) new JSONParser().parse(msg);

        String txnId = (String) jsonObject.get("txnId");
        String status = (String) jsonObject.get("status");

        TransactionOutboxEvent transactionOutboxEvent = transactionOutboxEventRepository.findByTxnIdAndEventType(txnId,
                TXN_COMPLETE_TOPIC);
        if (transactionOutboxEvent != null) {
            return;
        }

        TransactionStatus transactionStatus;

        if ("FAILED".equals(status)) {
            transactionStatus = TransactionStatus.FAILED;
        } else {
            transactionStatus = TransactionStatus.SUCCESSFUL;
        }

        Transaction transaction = transactionRepository.findByTxnId(txnId);
        transaction.setTransactionStatus(transactionStatus);

        transactionRepository.save(transaction);

        Integer receiverId = transaction.getReceiverUserId();
        Integer senderId = transaction.getSenderUserId();

        String senderEmail = "unknown";
        try {
            senderEmail = userClient.getUserEmail(senderId);
        } catch (Exception e) {
            // If sender doesn't exist, proceed with default
        }

        String receiverEmail = "unknown";
        try {
            receiverEmail = userClient.getUserEmail(receiverId);
        } catch (Exception e) {
            // If receiver doesn't exist, proceed with default
        }

        JSONObject txnCompleteEvent = new JSONObject();
        txnCompleteEvent.put("txnId", txnId);
        txnCompleteEvent.put("sender", senderEmail);
        txnCompleteEvent.put("receiver", receiverEmail);
        txnCompleteEvent.put("status", transaction.getTransactionStatus().name());
        txnCompleteEvent.put("amount", transaction.getAmount());

        // kafkaTemplate.send(TXN_COMPLETE_TOPIC, txnCompleteEvent.toJSONString());
        transactionOutboxEvent = new TransactionOutboxEvent();
        transactionOutboxEvent.setTxnId(txnId);
        transactionOutboxEvent.setEventType(TXN_COMPLETE_TOPIC);
        transactionOutboxEvent.setSent(false);
        transactionOutboxEvent.setPayload(txnCompleteEvent.toJSONString());

        transactionOutboxEventRepository.save(transactionOutboxEvent);

    }
}
