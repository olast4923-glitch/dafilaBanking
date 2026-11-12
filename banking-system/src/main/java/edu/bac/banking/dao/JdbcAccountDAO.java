package edu.bac.banking.dao;

import edu.bac.banking.model.Account;
import edu.bac.banking.model.AccountFactory;
import edu.bac.banking.model.AccountType;
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
import java.util.Locale;
import java.util.Optional;

/**
 * Single-table-inheritance persistence. The account_type discriminator is mapped
 * back to the right subclass by AccountFactory.restore(), which is where the
 * domain's polymorphism survives a database round-trip.
 */
public class JdbcAccountDAO implements AccountDAO {

    private static final String COLUMNS =
            "account_number, customer_id, account_type, balance, branch, interest_rate, "
                    + "employer_name, employer_address, opened_at";

    @Override
    public Account save(Account account) {
        String sql = "INSERT INTO accounts (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, account.getAccountNumber());
            ps.setLong(2, account.getCustomerId());
            ps.setString(3, account.getType().name());
            ps.setBigDecimal(4, account.getBalance());
            ps.setString(5, account.getBranch());
            ps.setBigDecimal(6, account.getMonthlyInterestRate());
            EmploymentInfo info = account.getEmploymentInfo();
            ps.setString(7, info == null ? null : info.getCompanyName());
            ps.setString(8, info == null ? null : info.getCompanyAddress());
            ps.setTimestamp(9, Timestamp.valueOf(account.getOpenedAt()));
            ps.executeUpdate();
            return account;
        } catch (SQLException ex) {
            throw new BankingException("Could not save account: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Account> findByNumber(String accountNumber) {
        List<Account> rows = queryMany("SELECT " + COLUMNS + " FROM accounts WHERE account_number = ?",
                ps -> ps.setString(1, accountNumber));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<Account> findByCustomerId(Long customerId) {
        return queryMany("SELECT " + COLUMNS + " FROM accounts WHERE customer_id = ? ORDER BY account_number",
                ps -> ps.setLong(1, customerId));
    }

    @Override
    public List<Account> findAll() {
        return queryMany("SELECT " + COLUMNS + " FROM accounts ORDER BY account_number", ps -> { });
    }

    @Override
    public void updateBalance(Account account) {
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE accounts SET balance = ? WHERE account_number = ?")) {
            ps.setBigDecimal(1, account.getBalance());
            ps.setString(2, account.getAccountNumber());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new BankingException("Could not update account balance: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(String accountNumber) {
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "DELETE FROM accounts WHERE account_number = ?")) {
            ps.setString(1, accountNumber);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new BankingException("Could not delete account: " + ex.getMessage(), ex);
        }
    }

    private List<Account> queryMany(String sql, Binder binder) {
        List<Account> result = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        } catch (SQLException ex) {
            throw new BankingException("Could not read accounts: " + ex.getMessage(), ex);
        }
        return result;
    }

    private Account map(ResultSet rs) throws SQLException {
        AccountType type = AccountType.valueOf(rs.getString("account_type").trim().toUpperCase(Locale.ROOT));
        String company = rs.getString("employer_name");
        EmploymentInfo info = (company == null || company.isBlank())
                ? null : new EmploymentInfo(company, rs.getString("employer_address"));
        return AccountFactory.restore(
                rs.getString("account_number"),
                rs.getLong("customer_id"),
                rs.getString("branch"),
                rs.getBigDecimal("balance"),
                rs.getTimestamp("opened_at").toLocalDateTime(),
                type,
                info);
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
