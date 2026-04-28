package com.example.jbdl.ewallet.services;

import javax.transaction.Transactional;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.jbdl.ewallet.entity.ProcessedWalletTransaction;
import com.example.jbdl.ewallet.entity.Wallet;
import com.example.jbdl.ewallet.entity.WalletOutboxEvent;
import com.example.jbdl.ewallet.repository.ProcessedWalletTransactionRepository;
import com.example.jbdl.ewallet.repository.WalletOutboxRepository;
import com.example.jbdl.ewallet.repository.WalletRepository;

@Service
public class WalletTransactionHandler {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ProcessedWalletTransactionRepository processedWalletTransactionRepository;

    @Autowired
    private WalletOutboxRepository walletOutboxRepository;

    @Transactional
    public void handleTransactionCreate(String txnId, Integer senderId, Integer receiverId, Double amount) {
        ProcessedWalletTransaction existing = processedWalletTransactionRepository.findById(txnId).orElse(null);
        if (existing != null) {
            return;
        }

        Wallet senderWallet = walletRepository.findByUserId(senderId);
        Wallet receiverWallet = walletRepository.findByUserId(receiverId);

        String status;
        if (senderWallet == null || receiverWallet == null) {
            status = "FAILED";
        } else if (senderWallet.getBalance() < amount) {
            status = "FAILED";
        } else {
            walletRepository.updateWallet(receiverId, amount);
            walletRepository.updateWallet(senderId, -amount);
            status = "SUCCESSFUL";
        }

        ProcessedWalletTransaction processed = new ProcessedWalletTransaction();
        processed.setTransId(txnId);
        processed.setStatus(status);
        processedWalletTransactionRepository.save(processed);

        JSONObject event = new JSONObject();
        event.put("txnId", txnId);
        event.put("status", status);

        WalletOutboxEvent outbox = new WalletOutboxEvent();
        outbox.setTxnId(txnId);
        outbox.setEventType("wallet_update");
        outbox.setPayload(event.toJSONString());
        outbox.setSent(false);
        walletOutboxRepository.save(outbox);
    }
}
