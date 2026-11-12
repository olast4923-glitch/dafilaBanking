package edu.bac.banking.model;

import edu.bac.banking.util.BankingException;
import edu.bac.banking.util.Money;
import edu.bac.banking.util.Validation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base class for all account types.
 *
 * Abstraction: declares getType(), getMonthlyInterestRate() and getDisplayName() as abstract.
 * Encapsulation: every field is private and the balance can only change through a
 * transaction-producing operation; there is no public setter.
 * Inheritance: SavingsAccount, InvestmentAccount and ChequeAccount extend this class.
 * Polymorphism: controllers hold List&lt;Account&gt; and call overridden behaviour on it.
 */
public abstract class Account implements Depositable {

    private final String accountNumber;
    private final Long customerId;
    private final String branch;
    private final LocalDateTime openedAt;
    private BigDecimal balance;

    protected Account(String accountNumber, Long customerId, String branch,
                      BigDecimal openingBalance, LocalDateTime openedAt) {
        this.accountNumber = Validation.required(accountNumber, "Account number");
        // Brief: "an account cannot exist without being held by a customer".
        this.customerId = Objects.requireNonNull(customerId,
                "An account cannot exist without being held by a customer.");
        this.branch = Validation.required(branch, "Branch");
        this.balance = Validation.nonNegative(openingBalance, "Opening balance");
        this.openedAt = openedAt == null ? LocalDateTime.now() : openedAt;
    }

    /** Overloaded deposit: the single-argument form delegates to the noted form. */
    @Override
    public final Transaction deposit(BigDecimal amount) {
        return deposit(amount, null);
    }

    @Override
    public Transaction deposit(BigDecimal amount, String note) {
        return changeBalance(Validation.positive(amount, "Deposit"), TransactionType.DEPOSIT, note);
    }

    /** Shared, guarded balance mutation. Subclasses never touch the balance field directly. */
    protected final Transaction changeBalance(BigDecimal signedAmount, TransactionType type, String note) {
        BigDecimal next = Money.scale(balance.add(signedAmount));
        if (next.signum() < 0) {
            throw new BankingException("Insufficient funds in " + accountNumber + ".");
        }
        balance = next;
        return new Transaction(accountNumber, type, signedAmount.abs(), balance, note, LocalDateTime.now());
    }

    protected final Transaction debit(BigDecimal amount, String note) {
        return changeBalance(Validation.positive(amount, "Withdrawal").negate(),
                TransactionType.WITHDRAWAL, note);
    }

    protected final Transaction postInterest(BigDecimal interest, String note) {
        if (interest == null || interest.signum() < 0) {
            throw new BankingException("Interest cannot be negative.");
        }
        return changeBalance(Money.scale(interest), TransactionType.INTEREST, note);
    }

    public abstract AccountType getType();

    public abstract BigDecimal getMonthlyInterestRate();

    public abstract String getDisplayName();

    public boolean allowsWithdrawals() {
        return this instanceof Withdrawable;
    }

    public boolean earnsInterest() {
        return this instanceof InterestBearing;
    }

    public String getAccountNumber() { return accountNumber; }
    public Long getCustomerId() { return customerId; }
    public String getBranch() { return branch; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public BigDecimal getBalance() { return balance; }

    public EmploymentInfo getEmploymentInfo() {
        return null;
    }

    @Override
    public String toString() {
        return accountNumber + " | " + getDisplayName() + " | " + Money.format(balance);
    }
}
