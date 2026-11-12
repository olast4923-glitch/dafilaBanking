package edu.bac.banking.controller;

import edu.bac.banking.dao.AccountDAO;
import edu.bac.banking.dao.CustomerDAO;
import edu.bac.banking.dao.Database;
import edu.bac.banking.dao.JdbcAccountDAO;
import edu.bac.banking.dao.JdbcCustomerDAO;
import edu.bac.banking.dao.JdbcTransactionDAO;
import edu.bac.banking.dao.TransactionDAO;

/**
 * Single wiring point shared by the web boundary, the JavaFX boundary and the tests.
 * Both UIs talk to the same controllers, which talk to the same DAOs.
 */
public final class BankContext {

    private final CustomerDAO customerDAO = new JdbcCustomerDAO();
    private final AccountDAO accountDAO = new JdbcAccountDAO();
    private final TransactionDAO transactionDAO = new JdbcTransactionDAO();

    private final LoginController loginController = new LoginController(customerDAO);
    private final AccountController accountController =
            new AccountController(accountDAO, customerDAO, transactionDAO);
    private final TransactionController transactionController = new TransactionController(transactionDAO);
    private final InterestController interestController =
            new InterestController(accountDAO, transactionDAO);

    private BankContext() {
    }

    public static BankContext start() {
        Database.initialize();
        return new BankContext();
    }

    public LoginController getLoginController() { return loginController; }
    public AccountController getAccountController() { return accountController; }
    public TransactionController getTransactionController() { return transactionController; }
    public InterestController getInterestController() { return interestController; }
    public CustomerDAO getCustomerDAO() { return customerDAO; }
    public AccountDAO getAccountDAO() { return accountDAO; }
    public TransactionDAO getTransactionDAO() { return transactionDAO; }
}
