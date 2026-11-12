package edu.bac.banking.dao;

import edu.bac.banking.model.Customer;

import java.util.List;
import java.util.Optional;

/** Data access contract. All JDBC lives in JdbcCustomerDAO, never in the model or the views. */
public interface CustomerDAO {

    Customer save(Customer customer);

    Optional<Customer> findById(Long id);

    Optional<Customer> findByEmail(String email);

    List<Customer> findAll();

    void update(Customer customer);

    void delete(Long id);
}
