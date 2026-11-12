package edu.bac.banking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Salary account. Deposits and withdrawals of any amount, no interest, and only
 * available to a working customer who supplies their employer's name and address.
 */
public final class ChequeAccount extends Account implements Withdrawable {

    private final EmploymentInfo employmentInfo;

    public ChequeAccount(String accountNumber, Long customerId, String branch,
                         BigDecimal openingBalance, EmploymentInfo employmentInfo,
                         LocalDateTime openedAt) {
        super(accountNumber, customerId, branch, openingBalance, openedAt);
        this.employmentInfo = Objects.requireNonNull(employmentInfo,
                "A cheque account requires the employer name and company address.");
    }

    @Override
    public AccountType getType() {
        return AccountType.CHEQUE;
    }

    @Override
    public BigDecimal getMonthlyInterestRate() {
        return BigDecimal.ZERO;
    }

    @Override
    public String getDisplayName() {
        return "Cheque account";
    }

    @Override
    public EmploymentInfo getEmploymentInfo() {
        return employmentInfo;
    }

    @Override
    public Transaction withdraw(BigDecimal amount) {
        return withdraw(amount, null);
    }

    @Override
    public Transaction withdraw(BigDecimal amount, String note) {
        return debit(amount, note);
    }

    @Override
    public String toString() {
        return super.toString() + " | employer: " + employmentInfo;
    }
}
