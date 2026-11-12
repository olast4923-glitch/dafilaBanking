package edu.bac.banking.model;

import java.math.BigDecimal;

/** Capability interface. The pair of overloads demonstrates overloading in the domain model. */
public interface Depositable {

    Transaction deposit(BigDecimal amount);

    Transaction deposit(BigDecimal amount, String note);
}
