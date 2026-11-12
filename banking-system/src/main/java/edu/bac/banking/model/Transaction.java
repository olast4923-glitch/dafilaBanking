package edu.bac.banking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable record of a single movement on an account. Produced by the core model, persisted by a DAO. */
public final class Transaction {

    private final Long id;
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String note;
    private final LocalDateTime occurredAt;

    public Transaction(String accountNumber, TransactionType type, BigDecimal amount,
                       BigDecimal balanceAfter, String note, LocalDateTime occurredAt) {
        this(null, accountNumber, type, amount, balanceAfter, note, occurredAt);
    }

    public Transaction(Long id, String accountNumber, TransactionType type, BigDecimal amount,
                       BigDecimal balanceAfter, String note, LocalDateTime occurredAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.note = note;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getNote() { return note; }
    public LocalDateTime getOccurredAt() { return occurredAt; }

    @Override
    public String toString() {
        return occurredAt + " | " + type + " | BWP " + amount.toPlainString()
                + " | balance BWP " + balanceAfter.toPlainString();
    }
}
