package com.example.jbdl.ewallet.repository;

import org.springframework.data.repository.CrudRepository;

import com.example.jbdl.ewallet.entity.ProcessedWalletTransaction;

public interface ProccessedWalletTransactionRepository extends CrudRepository<ProcessedWalletTransaction, String> {

}
