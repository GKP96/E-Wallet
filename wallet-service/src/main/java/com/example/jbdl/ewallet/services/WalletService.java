package com.example.jbdl.ewallet.services;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.jbdl.ewallet.entity.Wallet;
import com.example.jbdl.ewallet.repository.WalletRepository;

@Service
public class WalletService {

    @Value("${user.onboarding.amount}")
    private Double onboardingAmount;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionHandler walletTransactionHandler;

    private static final String USER_CREATE_TOPIC = "user_create";
    private static final String TXN_CREATE_TOPIC = "txn_create";

    @KafkaListener(topics = { USER_CREATE_TOPIC }, groupId = "jdbl61_grp")
    public void walletCreate(String message) throws Exception {

        JSONObject jsonObject = (JSONObject) new JSONParser().parse(message);

        if (!jsonObject.containsKey("userId")) {
            throw new Exception("userId is not present in the user event");
        }

        int userId = ((Long) jsonObject.get("userId")).intValue();

        Wallet existing = walletRepository.findByUserId(userId);
        if (existing != null) {
            return;
        }

        Wallet wallet = Wallet.builder()
                .balance(onboardingAmount)
                .userId(userId)
                .build();

        walletRepository.save(wallet);
    }

    @KafkaListener(topics = { TXN_CREATE_TOPIC }, groupId = "jbdl27_grp")
    public void walletUpdate(String msg) throws Exception {

        JSONObject jsonObject = (JSONObject) new JSONParser().parse(msg);

        if (!jsonObject.containsKey("sender") ||
                !jsonObject.containsKey("receiver") ||
                !jsonObject.containsKey("amount") ||
                !jsonObject.containsKey("txnId")) {
            throw new Exception("some of the details are not present in the txn create event");
        }

        Integer receiverId = ((Long) jsonObject.get("receiver")).intValue();
        Integer senderId = ((Long) jsonObject.get("sender")).intValue();
        Double amount = (Double) jsonObject.get("amount");
        String txnId = (String) jsonObject.get("txnId");
        walletTransactionHandler.handleTransactionCreate(txnId, senderId, receiverId, amount);

    }

}
