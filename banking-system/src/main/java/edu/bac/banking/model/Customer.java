package edu.bac.banking.model;

import edu.bac.banking.util.Validation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A customer may hold one or more accounts of any mix of types.
 * The account list is kept private and exposed as an unmodifiable view (encapsulation).
 */
public final class Customer {

    private Long id;
    private final String firstName;
    private final String surname;
    private final String address;
    private final String email;
    private final String passwordHash;
    private final boolean employed;
    private final EmploymentInfo employmentInfo;
    private final LocalDateTime createdAt;
    private final List<Account> accounts = new ArrayList<>();

    /** New customer, id assigned by CustomerDAO on insert. */
    public Customer(String firstName, String surname, String address, String email,
                    String passwordHash, boolean employed, EmploymentInfo employmentInfo) {
        this(null, firstName, surname, address, email, passwordHash, employed,
                employmentInfo, LocalDateTime.now());
    }

    /** Customer restored from a database row. */
    public Customer(Long id, String firstName, String surname, String address, String email,
                    String passwordHash, boolean employed, EmploymentInfo employmentInfo,
                    LocalDateTime createdAt) {
        this.id = id;
        this.firstName = Validation.required(firstName, "First name");
        this.surname = Validation.required(surname, "Surname");
        this.address = address;
        this.email = Validation.required(email, "Email");
        this.passwordHash = Validation.required(passwordHash, "Password hash");
        this.employed = employed;
        this.employmentInfo = employmentInfo;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public void assignId(Long generatedId) {
        if (this.id == null) {
            this.id = generatedId;
        }
    }

    public void addAccount(Account account) {
        if (account != null && !accounts.contains(account)) {
            accounts.add(account);
        }
    }

    public void loadAccounts(Collection<Account> loaded) {
        accounts.clear();
        if (loaded != null) {
            accounts.addAll(loaded);
        }
    }

    public Account findAccount(String accountNumber) {
        return accounts.stream()
                .filter(a -> a.getAccountNumber().equalsIgnoreCase(accountNumber))
                .findFirst()
                .orElse(null);
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getSurname() { return surname; }
    public String getFullName() { return firstName + " " + surname; }
    public String getAddress() { return address; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEmployed() { return employed; }
    public EmploymentInfo getEmploymentInfo() { return employmentInfo; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Unmodifiable view: callers cannot mutate the customer's accounts directly. */
    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    @Override
    public String toString() {
        return getFullName() + " <" + email + "> (" + accounts.size() + " accounts)";
    }
}
