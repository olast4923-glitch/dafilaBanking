package edu.bac.banking.util;

import java.math.BigDecimal;

/** Small validation helper so controllers and the model apply the same rules. */
public final class Validation {

    private Validation() {
    }

    public static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BankingException(field + " is required.");
        }
        return value.trim();
    }

    public static BigDecimal positive(BigDecimal amount, String operation) {
        if (amount == null || amount.signum() <= 0) {
            throw new BankingException(operation + " amount must be greater than zero.");
        }
        return Money.scale(amount);
    }

    public static BigDecimal nonNegative(BigDecimal amount, String field) {
        if (amount == null || amount.signum() < 0) {
            throw new BankingException(field + " cannot be negative.");
        }
        return Money.scale(amount);
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new BankingException(message);
        }
    }
}
