package edu.bac.banking.dao;

import edu.bac.banking.model.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionDAO {

    Transaction save(Transaction transaction);

    List<Transaction> findByAccountNumber(String accountNumber);

    List<Transaction> findAll();

    Optional<Transaction> findById(Long id);

    void delete(Long id);
}
