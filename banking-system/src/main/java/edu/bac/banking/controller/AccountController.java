package edu.bac.banking.controller;

import edu.bac.banking.dao.AccountDAO;
import edu.bac.banking.dao.CustomerDAO;
import edu.bac.banking.dao.TransactionDAO;
import edu.bac.banking.model.Account;
import edu.bac.banking.model.AccountFactory;
import edu.bac.banking.model.AccountType;
import edu.bac.banking.model.Customer;
import edu.bac.banking.model.EmploymentInfo;
import edu.bac.banking.model.Transaction;
import edu.bac.banking.model.TransactionType;
import edu.bac.banking.model.Withdrawable;
import edu.bac.banking.util.BankingException;
import edu.bac.banking.util.Money;
import edu.bac.banking.util.Validation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates account opening, deposits and withdrawals.
 * No GUI code here and no business logic in the views: the controller validates
 * input, invokes the core model and persists through the DAO layer.
 */
public class AccountController {

    private final AccountDAO accountDAO;
    private final CustomerDAO customerDAO;
    private final TransactionDAO transactionDAO;

    public AccountController(AccountDAO accountDAO, CustomerDAO customerDAO,
                             TransactionDAO transactionDAO) {
        this.accountDAO = accountDAO;
        this.customerDAO = customerDAO;
        this.transactionDAO = transactionDAO;
    }

    public Result<List<Account>> getAccounts(Long customerId) {
        try {
            return Result.success(accountDAO.findByCustomerId(customerId));
        } catch (RuntimeException ex) {
            return Result.failure(ex.getMessage());
        }
    }

    public Result<Account> openAccount(Long customerId, AccountType type, String openingAmount,
                                       String branch, String companyName, String companyAddress) {
        try {
            Customer customer = customerDAO.findById(customerId)
                    .orElseThrow(() -> new BankingException("Customer not found."));
            BigDecimal opening = Money.parse(openingAmount);
            EmploymentInfo supplied = (companyName != null && !companyName.isBlank())
                    ? new EmploymentInfo(companyName, companyAddress) : null;
            Account account = AccountFactory.open(customer, type, opening, branch, supplied);
            accountDAO.save(account);
            transactionDAO.save(new Transaction(account.getAccountNumber(), TransactionType.OPENING,
                    opening, account.getBalance(), "Account opened", LocalDateTime.now()));
            customer.addAccount(account);
            return Result.success(account.getType().getLabel() + " opened as " + account.getAccountNumber(), account);
        } catch (RuntimeException ex) {
            return Result.failure(ex.getMessage());
        }
    }

    /** Overloaded deposit forms; the short form delegates to the noted one. */
    public Result<Transaction> deposit(String accountNumber, String amount) {
        return deposit(accountNumber, amount, null);
    }

    public Result<Transaction> deposit(String accountNumber, String amount, String note) {
        try {
            Account account = find(accountNumber);
            Transaction txn = account.deposit(Money.parse(amount), note);
            persist(account, txn);
            return Result.success("Deposit of " + Money.format(txn.getAmount()) + " recorded.", txn);
        } catch (RuntimeException ex) {
            return Result.failure(ex.getMessage());
        }
    }

    public Result<Transaction> withdraw(String accountNumber, String amount) {
        return withdraw(accountNumber, amount, null);
    }

    public Result<Transaction> withdraw(String accountNumber, String amount, String note) {
        try {
            Account account = find(accountNumber);
            // The brief says a savings account does not allow withdrawals.
            if (!(account instanceof Withdrawable)) {
                return Result.failure("Savings accounts do not allow withdrawals.");
            }
            Transaction txn = ((Withdrawable) account).withdraw(Money.parse(amount), note);
            persist(account, txn);
            return Result.success("Withdrawal of " + Money.format(txn.getAmount()) + " recorded.", txn);
        } catch (RuntimeException ex) {
            return Result.failure(ex.getMessage());
        }
    }

    private Account find(String accountNumber) {
        return accountDAO.findByNumber(Validation.required(accountNumber, "Account number"))
                .orElseThrow(() -> new BankingException("Account " + accountNumber + " was not found."));
    }

    private void persist(Account account, Transaction txn) {
        accountDAO.updateBalance(account);
        transactionDAO.save(txn);
    }
}
