package com.example.jbdl.ewallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.example.jbdl.ewallet.entity.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    Wallet findByUserId(int userId);

    @Transactional
    @Modifying
    @Query("update Wallet w set w.balance = w.balance + ?2 where w.userId = ?1")
    void updateWallet(int userId, Double amount);


}
