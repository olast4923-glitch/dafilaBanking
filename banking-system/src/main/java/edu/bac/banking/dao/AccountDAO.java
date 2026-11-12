package edu.bac.banking.dao;

import edu.bac.banking.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountDAO {

    Account save(Account account);

    Optional<Account> findByNumber(String accountNumber);

    List<Account> findByCustomerId(Long customerId);

    List<Account> findAll();

    void updateBalance(Account account);

    void delete(String accountNumber);
}
