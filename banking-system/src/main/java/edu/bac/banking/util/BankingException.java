package edu.bac.banking.util;

/** Domain-level failure. Controllers translate this into a Result for either boundary layer. */
public class BankingException extends RuntimeException {

    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
