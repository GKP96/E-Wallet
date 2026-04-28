package com.example.jbdl.ewallet.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transaction_outbox", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "txnId", "eventType" })
})
@Getter
@Setter
public class TransactionOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String txnId;

    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private Boolean sent;

    @CreationTimestamp
    private Date createdOn;

    @Column(nullable = false)
    private int retryCount = 0;
}
