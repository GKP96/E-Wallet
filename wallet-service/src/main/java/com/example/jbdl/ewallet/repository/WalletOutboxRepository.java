package com.example.jbdl.ewallet.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.jbdl.ewallet.entity.WalletOutboxEvent;

public interface WalletOutboxRepository extends JpaRepository<WalletOutboxEvent, Long> {
    List<WalletOutboxEvent> findBySentFalseAndRetryCountLessThan(int retryCount);
}
