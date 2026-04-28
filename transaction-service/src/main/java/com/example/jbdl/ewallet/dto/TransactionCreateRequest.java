package com.example.jbdl.ewallet.dto;

import lombok.*;

import javax.validation.constraints.Positive;

import com.example.jbdl.ewallet.entity.Transaction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionCreateRequest {

    @NonNull
    private String idempotencyKey;

    @Positive
    private Integer senderUserId;

    @Positive
    private Integer receiverUserId;

    @Positive
    private Double amount;

    private String purpose;

    public Transaction to() {
        return Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .senderUserId(senderUserId)
                .receiverUserId(receiverUserId)
                .amount(amount)
                .purpose(purpose)
                .build();
    }
}
