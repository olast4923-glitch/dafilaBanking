package edu.bac.banking.controller;

import edu.bac.banking.dao.AccountDAO;
import edu.bac.banking.dao.TransactionDAO;
import edu.bac.banking.model.Account;
import edu.bac.banking.model.InterestBearing;
import edu.bac.banking.model.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Pays monthly interest to the appropriate accounts only.
 * This is the polymorphism showcase: the controller works with List&lt;Account&gt;,
 * and each concrete account computes its own rate through the InterestBearing interface.
 * The state diagram for the assignment's PayInterest use case models this flow.
 */
public class InterestController {

    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;

    public InterestController(AccountDAO accountDAO, TransactionDAO transactionDAO) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
    }

    public Result<List<Transaction>> payMonthlyInterest() {
        try {
            List<Transaction> posted = new ArrayList<>();
            for (Account account : accountDAO.findAll()) {
                if (account instanceof InterestBearing interestAccount) {
                    Transaction txn = interestAccount.applyMonthlyInterest();
                    accountDAO.updateBalance(account);
                    transactionDAO.save(txn);
                    posted.add(txn);
                }
            }
            return Result.success("Monthly interest paid to " + posted.size() + " account(s).", posted);
        } catch (RuntimeException ex) {
            return Result.failure(ex.getMessage());
        }
    }
}
