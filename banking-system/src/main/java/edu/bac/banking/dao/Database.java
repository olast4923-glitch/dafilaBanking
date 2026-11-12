package edu.bac.banking.dao;

import edu.bac.banking.util.BankingException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Connection provider and schema bootstrap.
 *
 * Priority order:
 *   1. -Dbank.db.url=...             (integration tests, explicit override)
 *   2. SUPABASE_DB_URL + user/pass   (GitHub Codespaces secrets / local .env)
 *   3. DB_URL or H2 in-memory        (offline fallback, also what a marker sees
 *                                     when they open the Codespace without secrets)
 *
 * The SQL is written once and runs unchanged on PostgreSQL and H2.
 */
public final class Database {

    private static final String DEFAULT_H2 = "jdbc:h2:mem:banking;DB_CLOSE_DELAY=-1";

    private static volatile boolean initialized = false;

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        String forced = System.getProperty("bank.db.url");
        if (forced != null && !forced.isBlank()) {
            return DriverManager.getConnection(forced,
                    System.getProperty("bank.db.user", "sa"),
                    System.getProperty("bank.db.password", ""));
        }

        String supabaseUrl = System.getenv("SUPABASE_DB_URL");
        if (supabaseUrl != null && !supabaseUrl.isBlank()) {
            String user = System.getenv("SUPABASE_DB_USER");
            String password = System.getenv("SUPABASE_DB_PASSWORD");
            if (user == null || password == null) {
                throw new SQLException("SUPABASE_DB_USER and SUPABASE_DB_PASSWORD must be set "
                        + "whenever SUPABASE_DB_URL is set.");
            }
            // Use the Supabase session pooler host (IPv4) with ?sslmode=require.
            return DriverManager.getConnection(supabaseUrl, user, password);
        }

        String url = System.getenv().getOrDefault("DB_URL", DEFAULT_H2);
        String user = System.getenv().getOrDefault("DB_USER", "sa");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }

    public static String activeTarget() {
        if (System.getProperty("bank.db.url") != null) {
            return "JDBC override (tests)";
        }
        return System.getenv("SUPABASE_DB_URL") != null
                ? "Supabase PostgreSQL" : "H2 in-memory (offline fallback)";
    }

    /** Creates the schema when missing and loads the 10+ sample records when empty. */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        try {
            if (!customersTableExists()) {
                runScript("/db/schema.sql");
            }
            if (customerCount() == 0) {
                runScript("/db/seed.sql");
            }
            initialized = true;
        } catch (SQLException | IOException ex) {
            throw new BankingException("Database initialisation failed: " + ex.getMessage(), ex);
        }
    }

    /** Allows a test to point at a fresh H2 database and re-bootstrap. */
    public static synchronized void reset() {
        initialized = false;
    }

    private static boolean customersTableExists() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM customers")) {
            return rs.next();
        } catch (SQLException ex) {
            return false;
        }
    }

    private static long customerCount() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM customers")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static void runScript(String resource) throws SQLException, IOException {
        try (InputStream in = Database.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Missing classpath resource " + resource);
            }
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            StringBuilder cleaned = new StringBuilder();
            for (String line : sql.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                cleaned.append(line).append('\n');
            }
            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                for (String part : cleaned.toString().split(";")) {
                    String command = part.trim();
                    if (!command.isEmpty()) {
                        statement.execute(command);
                    }
                }
            }
        }
    }
}
