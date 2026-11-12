package edu.bac.banking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 0.05% monthly interest, deposits only. This class intentionally does NOT
 * implement Withdrawable, so the "no withdrawals" rule is enforced by the type system.
 */
public final class SavingsAccount extends Account implements InterestBearing {

    /** 0.05% per month = 0.0005 of the balance. */
    public static final BigDecimal MONTHLY_RATE = new BigDecimal("0.000500");

    public SavingsAccount(String accountNumber, Long customerId, String branch,
                          BigDecimal openingBalance, LocalDateTime openedAt) {
        super(accountNumber, customerId, branch, openingBalance, openedAt);
    }

    @Override
    public AccountType getType() {
        return AccountType.SAVINGS;
    }

    @Override
    public BigDecimal getMonthlyInterestRate() {
        return MONTHLY_RATE;
    }

    @Override
    public String getDisplayName() {
        return "Savings account";
    }

    @Override
    public Transaction applyMonthlyInterest() {
        return postInterest(getBalance().multiply(MONTHLY_RATE), "Monthly interest at 0.05%");
    }

    @Override
    public String toString() {
        return super.toString() + " | withdrawals not permitted";
    }
}
