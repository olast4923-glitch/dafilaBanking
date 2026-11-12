package edu.bac.banking.dao;

import edu.bac.banking.model.Customer;
import edu.bac.banking.model.EmploymentInfo;
import edu.bac.banking.util.BankingException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCustomerDAO implements CustomerDAO {

    private static final String COLUMNS =
            "id, first_name, surname, address, email, password_hash, employed, "
                    + "employer_name, employer_address, created_at";

    @Override
    public Customer save(Customer customer) {
        String sql = "INSERT INTO customers (first_name, surname, address, email, password_hash, "
                + "employed, employer_name, employer_address, created_at) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, customer, false);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    customer.assignId(keys.getLong(1));
                }
            }
            return customer;
        } catch (SQLException ex) {
            throw new BankingException("Could not save customer: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return queryOne("SELECT " + COLUMNS + " FROM customers WHERE id = ?", ps -> ps.setLong(1, id));
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return queryOne("SELECT " + COLUMNS + " FROM customers WHERE email = ?",
                ps -> ps.setString(1, email));
    }

    @Override
    public List<Customer> findAll() {
        return queryMany("SELECT " + COLUMNS + " FROM customers ORDER BY id", ps -> { });
    }

    @Override
    public void update(Customer customer) {
        String sql = "UPDATE customers SET first_name=?, surname=?, address=?, email=?, "
                + "password_hash=?, employed=?, employer_name=?, employer_address=? WHERE id=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, customer, true);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new BankingException("Could not update customer: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(Long id) {
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM customers WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new BankingException("Could not delete customer: " + ex.getMessage(), ex);
        }
    }

    private void bind(PreparedStatement ps, Customer customer, boolean forUpdate) throws SQLException {
        EmploymentInfo info = customer.getEmploymentInfo();
        ps.setString(1, customer.getFirstName());
        ps.setString(2, customer.getSurname());
        ps.setString(3, customer.getAddress());
        ps.setString(4, customer.getEmail());
        ps.setString(5, customer.getPasswordHash());
        ps.setBoolean(6, customer.isEmployed());
        ps.setString(7, info == null ? null : info.getCompanyName());
        ps.setString(8, info == null ? null : info.getCompanyAddress());
        if (forUpdate) {
            ps.setLong(9, customer.getId());
        } else {
            ps.setTimestamp(9, Timestamp.valueOf(customer.getCreatedAt()));
        }
    }

    private Optional<Customer> queryOne(String sql, Binder binder) {
        List<Customer> rows = queryMany(sql, binder);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private List<Customer> queryMany(String sql, Binder binder) {
        List<Customer> result = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        } catch (SQLException ex) {
            throw new BankingException("Could not read customers: " + ex.getMessage(), ex);
        }
        return result;
    }

    private Customer map(ResultSet rs) throws SQLException {
        String company = rs.getString("employer_name");
        EmploymentInfo info = (company == null || company.isBlank())
                ? null : new EmploymentInfo(company, rs.getString("employer_address"));
        return new Customer(
                rs.getLong("id"),
                rs.getString("first_name"),
                rs.getString("surname"),
                rs.getString("address"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getBoolean("employed"),
                info,
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
