package edu.bac.banking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 5% monthly interest, deposits and withdrawals, minimum opening deposit of BWP 500.00. */
public final class InvestmentAccount extends Account implements InterestBearing, Withdrawable {

    public static final BigDecimal MIN_OPENING_BALANCE = new BigDecimal("500.00");

    /** 5% per month = 0.05 of the balance. */
    public static final BigDecimal MONTHLY_RATE = new BigDecimal("0.050000");

    public InvestmentAccount(String accountNumber, Long customerId, String branch,
                             BigDecimal openingBalance, LocalDateTime openedAt) {
        // The BWP 500.00 rule applies to the OPENING deposit only, so it is enforced
        // in AccountFactory.open, not here. A restored account may hold less after withdrawals.
        super(accountNumber, customerId, branch, openingBalance, openedAt);
    }

    @Override
    public AccountType getType() {
        return AccountType.INVESTMENT;
    }

    @Override
    public BigDecimal getMonthlyInterestRate() {
        return MONTHLY_RATE;
    }

    @Override
    public String getDisplayName() {
        return "Investment account";
    }

    @Override
    public Transaction applyMonthlyInterest() {
        return postInterest(getBalance().multiply(MONTHLY_RATE), "Monthly interest at 5%");
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
        return super.toString() + " | 5% monthly interest";
    }
}
