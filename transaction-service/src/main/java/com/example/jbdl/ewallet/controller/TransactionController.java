package com.example.jbdl.ewallet.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.jbdl.ewallet.dto.TransactionCreateRequest;
import com.example.jbdl.ewallet.service.TransactionService;

import javax.validation.Valid;

@RestController
public class TransactionController {

    @Autowired
    TransactionService transactionService;

    @PostMapping("/transact")
    public String createTxn(@RequestHeader("X-Idempotency-Key") String idempotencyKey, @Valid @RequestBody TransactionCreateRequest transactionCreateRequest){
        transactionCreateRequest.setIdempotencyKey(idempotencyKey);
        String txnId = transactionService.createTxn(transactionCreateRequest.to());
        return "Your transaction has been initiated, here's the transaction id : " + txnId;
    }
}
