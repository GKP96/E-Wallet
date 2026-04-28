package com.example.jbdl.ewallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.jbdl.ewallet.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    Transaction findByTxnId(String txnId);

    Transaction findByIdempotencyKey(String idempotencyKey);

}
