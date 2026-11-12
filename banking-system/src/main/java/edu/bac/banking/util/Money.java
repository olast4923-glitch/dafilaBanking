package edu.bac.banking.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/** All monetary values are BigDecimal in Botswana Pula (BWP). Never use double for money. */
public final class Money {

    public static final Currency CURRENCY = Currency.getInstance("BWP");
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    private Money() {
    }

    public static BigDecimal scale(BigDecimal value) {
        if (value == null) {
            throw new BankingException("A monetary value is required.");
        }
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal parse(String value) {
        try {
            return scale(new BigDecimal(value.trim()));
        } catch (NumberFormatException | NullPointerException ex) {
            throw new BankingException("'" + value + "' is not a valid amount.");
        }
    }

    public static String format(BigDecimal value) {
        return CURRENCY.getCurrencyCode() + " " + scale(value).toPlainString();
    }
}
