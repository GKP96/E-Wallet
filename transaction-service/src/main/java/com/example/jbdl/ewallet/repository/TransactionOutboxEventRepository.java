package com.example.jbdl.ewallet.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.example.jbdl.ewallet.entity.TransactionOutboxEvent;

public interface TransactionOutboxEventRepository extends CrudRepository<TransactionOutboxEvent, Long> {

    List<TransactionOutboxEvent> findBySentFalseAndRetryCountLessThan(int retryCount);

    TransactionOutboxEvent findByTxnIdAndEventType(String txnId, String eventType);

}
