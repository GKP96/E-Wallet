package com.example.jbdl.ewallet.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "processed_wallet_transactions")
@Setter
@Getter
public class ProcessedWalletTransaction {
    @Id
    private String transId;
    private String status;
}
