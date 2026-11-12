package edu.bac.banking.boundary.web;

import edu.bac.banking.model.Account;
import edu.bac.banking.model.Customer;
import edu.bac.banking.model.Transaction;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps domain objects to plain maps for JSON. Keeping this in the boundary layer
 * means Gson never has to reflect over the inheritance hierarchy or Java time types.
 */
public final class JsonViews {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private JsonViews() {
    }

    public static Map<String, Object> success(String message, Object data) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("message", message);
        out.put("data", data);
        return out;
    }

    public static Map<String, Object> failure(String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", false);
        out.put("message", message);
        out.put("data", null);
        return out;
    }

    public static Map<String, Object> customer(Customer customer) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", customer.getId());
        out.put("fullName", customer.getFullName());
        out.put("firstName", customer.getFirstName());
        out.put("email", customer.getEmail());
        out.put("address", customer.getAddress());
        out.put("employed", customer.isEmployed());
        out.put("employer", customer.getEmploymentInfo() == null
                ? null : customer.getEmploymentInfo().toString());
        out.put("accounts", accounts(customer.getAccounts()));
        return out;
    }

    public static List<Map<String, Object>> accounts(List<Account> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Account account : list) {
            out.add(account(account));
        }
        return out;
    }

    public static Map<String, Object> account(Account account) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accountNumber", account.getAccountNumber());
        out.put("type", account.getType().name());
        out.put("displayName", account.getDisplayName());
        out.put("balance", account.getBalance().toPlainString());
        out.put("branch", account.getBranch());
        out.put("interestLabel", percent(account.getMonthlyInterestRate()) + " / month");
        out.put("allowsWithdrawals", account.allowsWithdrawals());
        out.put("earnsInterest", account.earnsInterest());
        out.put("employer", account.getEmploymentInfo() == null
                ? null : account.getEmploymentInfo().toString());
        return out;
    }

    public static Map<String, Object> transaction(Transaction txn) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", txn.getId());
        row.put("accountNumber", txn.getAccountNumber());
        row.put("type", txn.getType().name());
        row.put("amount", txn.getAmount().toPlainString());
        row.put("balanceAfter", txn.getBalanceAfter().toPlainString());
        row.put("note", txn.getNote());
        row.put("occurredAt", txn.getOccurredAt().format(ISO));
        return row;
    }

    public static List<Map<String, Object>> transactions(List<Transaction> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Transaction txn : list) {
            out.add(transaction(txn));
        }
        return out;
    }

    private static String percent(BigDecimal rate) {
        return rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }
}
