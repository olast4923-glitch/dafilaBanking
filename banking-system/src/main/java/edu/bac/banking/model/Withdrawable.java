package edu.bac.banking.model;

import java.math.BigDecimal;

/**
 * Capability interface. SavingsAccount deliberately does NOT implement this,
 * which makes "no withdrawals" a compile-time property of the type system.
 */
public interface Withdrawable {

    Transaction withdraw(BigDecimal amount);

    Transaction withdraw(BigDecimal amount, String note);
}
