package edu.bac.banking.dao;

import edu.bac.banking.model.Transaction;
import edu.bac.banking.model.TransactionType;
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

public class JdbcTransactionDAO implements TransactionDAO {

    private static final String COLUMNS =
            "id, account_number, txn_type, amount, balance_after, note, occurred_at";

    @Override
    public Transaction save(Transaction transaction) {
        String sql = "INSERT INTO transactions (account_number, txn_type, amount, balance_after, "
                + "note, occurred_at) VALUES (?,?,?,?,?,?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, transaction.getAccountNumber());
            ps.setString(2, transaction.getType().name());
            ps.setBigDecimal(3, transaction.getAmount());
            ps.setBigDecimal(4, transaction.getBalanceAfter());
            ps.setString(5, transaction.getNote());
            ps.setTimestamp(6, Timestamp.valueOf(transaction.getOccurredAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return withId(transaction, keys.getLong(1));
                }
            }
            return transaction;
        } catch (SQLException ex) {
            throw new BankingException("Could not save transaction: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Transaction> findByAccountNumber(String accountNumber) {
        return queryMany("SELECT " + COLUMNS + " FROM transactions WHERE account_number = ? ORDER BY id DESC",
                ps -> ps.setString(1, accountNumber));
    }

    @Override
    public List<Transaction> findAll() {
        return queryMany("SELECT " + COLUMNS + " FROM transactions ORDER BY id DESC", ps -> { });
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        List<Transaction> rows = queryMany("SELECT " + COLUMNS + " FROM transactions WHERE id = ?",
                ps -> ps.setLong(1, id));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public void delete(Long id) {
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new BankingException("Could not delete transaction: " + ex.getMessage(), ex);
        }
    }

    private Transaction withId(Transaction transaction, Long id) {
        return new Transaction(id, transaction.getAccountNumber(), transaction.getType(),
                transaction.getAmount(), transaction.getBalanceAfter(), transaction.getNote(),
                transaction.getOccurredAt());
    }

    private List<Transaction> queryMany(String sql, Binder binder) {
        List<Transaction> result = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        } catch (SQLException ex) {
            throw new BankingException("Could not read transactions: " + ex.getMessage(), ex);
        }
        return result;
    }

    private Transaction map(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getLong("id"),
                rs.getString("account_number"),
                TransactionType.valueOf(rs.getString("txn_type").trim().toUpperCase(Locale.ROOT)),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("balance_after"),
                rs.getString("note"),
                rs.getTimestamp("occurred_at").toLocalDateTime());
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
