package edu.bac.banking.model;

import java.math.BigDecimal;

/** Capability interface implemented only by SavingsAccount and InvestmentAccount. */
public interface InterestBearing {

    BigDecimal getMonthlyInterestRate();

    Transaction applyMonthlyInterest();
}
