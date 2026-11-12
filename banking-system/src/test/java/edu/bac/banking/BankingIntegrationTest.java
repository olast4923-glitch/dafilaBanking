package edu.bac.banking;

import edu.bac.banking.controller.AccountController;
import edu.bac.banking.controller.BankContext;
import edu.bac.banking.controller.Result;
import edu.bac.banking.dao.Database;
import edu.bac.banking.model.Account;
import edu.bac.banking.model.AccountType;
import edu.bac.banking.model.ChequeAccount;
import edu.bac.banking.model.Customer;
import edu.bac.banking.model.InterestBearing;
import edu.bac.banking.model.InvestmentAccount;
import edu.bac.banking.model.SavingsAccount;
import edu.bac.banking.model.Transaction;
import edu.bac.banking.model.Withdrawable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-stack integration test: boundary flow (login) -> controller -> core model
 * -> JDBC DAO -> database, and back. Runs on its own H2 in-memory database so it
 * never depends on Supabase credentials.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BankingIntegrationTest {

    private static BankContext context;

    @BeforeAll
    static void setUp() {
        System.setProperty("bank.db.url", "jdbc:h2:mem:banking_it;DB_CLOSE_DELAY=-1");
        Database.reset();
        context = BankContext.start();
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty("bank.db.url");
        Database.reset();
    }

    @Test
    @Order(1)
    void sampleRecordsAreLoaded() {
        assertTrue(context.getCustomerDAO().findAll().size() >= 10, "at least 10 sample customers");
        assertTrue(context.getAccountDAO().findAll().size() >= 10, "at least 10 sample accounts");
        assertTrue(context.getTransactionDAO().findAll().size() >= 10, "at least 10 sample transactions");
    }

    @Test
    @Order(2)
    void aCustomerCanHoldAllThreeAccountTypes() {
        Customer dafila = context.getCustomerDAO()
                .findByEmail("dafila.philimon@example.com").orElseThrow();
        assertEquals("Dafila Philimon", dafila.getFullName());
        List<Account> accounts = context.getAccountDAO().findByCustomerId(dafila.getId());
        assertEquals(3, accounts.size());
        assertTrue(accounts.stream().anyMatch(a -> a instanceof SavingsAccount));
        assertTrue(accounts.stream().anyMatch(a -> a instanceof InvestmentAccount));
        assertTrue(accounts.stream().anyMatch(a -> a instanceof ChequeAccount));
    }

    @Test
    @Order(3)
    void fullIntegrationFlow() {
        // 1. Boundary -> controller: register and log in.
        Result<Customer> registration = context.getLoginController().register(
                "Integration", "Tester", "Gaborone", "integration.tester@example.com",
                "Passw0rd!", true, "BAC", "Notwane Road");
        assertTrue(registration.isOk(), registration.getMessage());
        Long customerId = registration.getData().getId();

        Result<Customer> login = context.getLoginController()
                .login("integration.tester@example.com", "Passw0rd!");
        assertTrue(login.isOk(), login.getMessage());
        assertFalse(context.getLoginController().login("integration.tester@example.com", "wrong").isOk());

        // 2. Controller -> core model -> DAO: open accounts and enforce the rules.
        Result<Account> savings = context.getAccountController().openAccount(
                customerId, AccountType.SAVINGS, "100.00", "Integration Branch", null, null);
        assertTrue(savings.isOk(), savings.getMessage());

        Result<Account> tooSmall = context.getAccountController().openAccount(
                customerId, AccountType.INVESTMENT, "100.00", "Integration Branch", null, null);
        assertFalse(tooSmall.isOk(), "investment accounts require BWP 500.00 to open");

        Result<Account> investment = context.getAccountController().openAccount(
                customerId, AccountType.INVESTMENT, "500.00", "Integration Branch", null, null);
        assertTrue(investment.isOk(), investment.getMessage());

        Result<Account> cheque = context.getAccountController().openAccount(
                customerId, AccountType.CHEQUE, "0.00", "Integration Branch", "BAC", "Notwane Road");
        assertTrue(cheque.isOk(), cheque.getMessage());
        assertTrue(cheque.getData() instanceof Withdrawable);
        assertFalse(cheque.getData() instanceof InterestBearing);

        String savingsNumber = savings.getData().getAccountNumber();
        String investmentNumber = investment.getData().getAccountNumber();

        // 3. Deposit and withdraw through the controllers.
        Result<Transaction> deposit = context.getAccountController().deposit(
                investmentNumber, "250.00", "integration deposit");
        assertTrue(deposit.isOk(), deposit.getMessage());
        assertEquals(new BigDecimal("750.00"), deposit.getData().getBalanceAfter());

        Result<Transaction> blocked = context.getAccountController().withdraw(savingsNumber, "10.00");
        assertFalse(blocked.isOk(), "savings accounts do not allow withdrawals");

        Result<Transaction> withdrawal = context.getAccountController().withdraw(
                investmentNumber, "50.00", "integration withdrawal");
        assertTrue(withdrawal.isOk(), withdrawal.getMessage());
        assertEquals(new BigDecimal("700.00"), withdrawal.getData().getBalanceAfter());

        // 4. Polymorphic interest payment: only interest-bearing accounts are touched.
        Result<List<Transaction>> interest = context.getInterestController().payMonthlyInterest();
        assertTrue(interest.isOk(), interest.getMessage());

        // 5. Verify every change reached the JDBC layer.
        Account reloadedInvestment = context.getAccountDAO()
                .findByNumber(investmentNumber).orElseThrow();
        assertTrue(reloadedInvestment instanceof InvestmentAccount, "subclass survives a DB round-trip");
        assertEquals(new BigDecimal("735.00"), reloadedInvestment.getBalance());

        Account reloadedSavings = context.getAccountDAO()
                .findByNumber(savingsNumber).orElseThrow();
        assertEquals(new BigDecimal("100.05"), reloadedSavings.getBalance());

        List<Transaction> history = context.getTransactionDAO().findByAccountNumber(investmentNumber);
        assertTrue(history.size() >= 4, "opening, deposit, withdrawal and interest are all persisted");
        assertTrue(history.stream().anyMatch(t -> t.getType().name().equals("INTEREST")));
    }
}
