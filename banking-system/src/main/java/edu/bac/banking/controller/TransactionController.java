package edu.bac.banking.controller;

import edu.bac.banking.dao.TransactionDAO;
import edu.bac.banking.model.Transaction;
import edu.bac.banking.util.Validation;

import java.util.List;

/** Read-only view of transaction history for a chosen account. */
public class TransactionController {

    private final TransactionDAO transactionDAO;

    public TransactionController(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    public Result<List<Transaction>> history(String accountNumber) {
        try {
            String number = Validation.required(accountNumber, "Account number");
            return Result.success(transactionDAO.findByAccountNumber(number));
        } catch (RuntimeException ex) {
            return Result.failure(ex.getMessage());
        }
    }

    public Result<List<Transaction>> allTransactions() {
        try {
            return Result.success(transactionDAO.findAll());
        } catch (RuntimeException ex) {
            return Result.failure(ex.getMessage());
        }
    }
}
