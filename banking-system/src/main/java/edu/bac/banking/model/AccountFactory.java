package edu.bac.banking.model;

import edu.bac.banking.util.Money;
import edu.bac.banking.util.Validation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Creates the correct Account subclass for a requested account type and rebuilds
 * subclasses from persisted rows. This is where polymorphism enters the persistence path.
 */
public final class AccountFactory {

    private static final AtomicLong SEQUENCE = new AtomicLong(2000);

    private AccountFactory() {
    }

    /** Overloaded opening: employment details default to the customer's own record. */
    public static Account open(Customer customer, AccountType type, BigDecimal openingBalance,
                               String branch) {
        return open(customer, type, openingBalance, branch, null);
    }

    public static Account open(Customer customer, AccountType type, BigDecimal openingBalance,
                               String branch, EmploymentInfo suppliedEmployment) {
        Objects.requireNonNull(customer, "A customer is required to open an account.");
        Objects.requireNonNull(type, "An account type is required.");
        Validation.require(customer.getId() != null, "Save the customer before opening an account.");

        BigDecimal opening = Validation.nonNegative(openingBalance, "Opening balance");
        String number = "ACC-" + SEQUENCE.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();

        switch (type) {
            case SAVINGS:
                return new SavingsAccount(number, customer.getId(), branch, opening, now);
            case INVESTMENT:
                Validation.require(opening.compareTo(InvestmentAccount.MIN_OPENING_BALANCE) >= 0,
                        "An investment account requires an initial deposit of at least "
                                + Money.format(InvestmentAccount.MIN_OPENING_BALANCE) + ".");
                return new InvestmentAccount(number, customer.getId(), branch, opening, now);
            case CHEQUE:
                Validation.require(customer.isEmployed(),
                        "A cheque account can only be opened for a person who is working.");
                EmploymentInfo info = suppliedEmployment != null
                        ? suppliedEmployment : customer.getEmploymentInfo();
                Validation.require(info != null,
                        "A cheque account requires the employer name and company address.");
                return new ChequeAccount(number, customer.getId(), branch, opening, info, now);
            default:
                throw new IllegalArgumentException("Unknown account type: " + type);
        }
    }

    /** Rebuilds a concrete account from the single-table-inheritance row. */
    public static Account restore(String accountNumber, Long customerId, String branch,
                                  BigDecimal balance, LocalDateTime openedAt,
                                  AccountType type, EmploymentInfo employmentInfo) {
        Objects.requireNonNull(type, "A stored account type is required.");
        switch (type) {
            case SAVINGS:
                return new SavingsAccount(accountNumber, customerId, branch, balance, openedAt);
            case INVESTMENT:
                return new InvestmentAccount(accountNumber, customerId, branch, balance, openedAt);
            case CHEQUE:
                EmploymentInfo info = employmentInfo != null
                        ? employmentInfo : new EmploymentInfo("Not recorded", "Not recorded");
                return new ChequeAccount(accountNumber, customerId, branch, balance, info, openedAt);
            default:
                throw new IllegalArgumentException("Unknown account type: " + type);
        }
    }
}
