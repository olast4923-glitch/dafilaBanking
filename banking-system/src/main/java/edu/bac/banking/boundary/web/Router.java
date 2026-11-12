package edu.bac.banking.boundary.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.bac.banking.controller.BankContext;
import edu.bac.banking.controller.Result;
import edu.bac.banking.dao.Database;
import edu.bac.banking.model.Account;
import edu.bac.banking.model.AccountType;
import edu.bac.banking.model.Customer;
import edu.bac.banking.model.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Boundary class for the Codespaces browser UI.
 * It contains no business logic: it parses HTTP, calls a controller and serialises
 * the Result. All rules live in the model/controller/DAO layers.
 */
public final class Router implements HttpHandler {

    private static final Gson GSON = new Gson();

    private final BankContext context;

    public Router(BankContext context) {
        this.context = context;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("OPTIONS".equalsIgnoreCase(method)) {
                sendJson(exchange, 200, new LinkedHashMap<>());
            } else if (path.startsWith("/api/")) {
                handleApi(exchange, path, method);
            } else {
                serveStatic(exchange, path);
            }
        } catch (Exception ex) {
            sendJson(exchange, 500, JsonViews.failure("Server error: " + ex.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private void handleApi(HttpExchange exchange, String path, String method) throws IOException {
        JsonObject body = "POST".equalsIgnoreCase(method) ? body(exchange) : new JsonObject();
        Map<String, String> query = query(exchange.getRequestURI().getRawQuery());

        switch (path) {
            case "/api/health": {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("application", "Meridian Bank (CSE202 Banking System)");
                info.put("database", Database.activeTarget());
                sendJson(exchange, 200, JsonViews.success("Running", info));
                break;
            }
            case "/api/login": {
                Result<Customer> result = context.getLoginController()
                        .login(str(body, "email"), str(body, "password"));
                if (!result.isOk()) {
                    sendJson(exchange, 401, JsonViews.failure(result.getMessage()));
                    return;
                }
                Customer customer = result.getData();
                Result<List<Account>> accounts = context.getAccountController()
                        .getAccounts(customer.getId());
                if (accounts.isOk()) {
                    customer.loadAccounts(accounts.getData());
                }
                sendJson(exchange, 200,
                        JsonViews.success(result.getMessage(), JsonViews.customer(customer)));
                break;
            }
            case "/api/register": {
                Result<Customer> result = context.getLoginController().register(
                        str(body, "firstName"), str(body, "surname"), str(body, "address"),
                        str(body, "email"), str(body, "password"), bool(body, "employed"),
                        str(body, "companyName"), str(body, "companyAddress"));
                send(result, exchange, JsonViews::customer);
                break;
            }
            case "/api/accounts": {
                Long customerId = longFrom(query.get("customerId"));
                if (customerId == null) {
                    sendJson(exchange, 400, JsonViews.failure("customerId is required."));
                    return;
                }
                Result<List<Account>> result = context.getAccountController().getAccounts(customerId);
                if (result.isOk()) {
                    sendJson(exchange, 200,
                            JsonViews.success("OK", JsonViews.accounts(result.getData())));
                } else {
                    sendJson(exchange, 400, JsonViews.failure(result.getMessage()));
                }
                break;
            }
            case "/api/accounts/open": {
                AccountType type = parseType(str(body, "type"));
                if (type == null) {
                    sendJson(exchange, 400,
                            JsonViews.failure("type must be SAVINGS, INVESTMENT or CHEQUE."));
                    return;
                }
                Result<Account> result = context.getAccountController().openAccount(
                        longFrom(str(body, "customerId")), type, str(body, "openingBalance"),
                        str(body, "branch"), str(body, "companyName"), str(body, "companyAddress"));
                send(result, exchange, JsonViews::account);
                break;
            }
            case "/api/transactions/deposit": {
                Result<Transaction> result = context.getAccountController().deposit(
                        str(body, "accountNumber"), str(body, "amount"), str(body, "note"));
                send(result, exchange, JsonViews::transaction);
                break;
            }
            case "/api/transactions/withdraw": {
                Result<Transaction> result = context.getAccountController().withdraw(
                        str(body, "accountNumber"), str(body, "amount"), str(body, "note"));
                send(result, exchange, JsonViews::transaction);
                break;
            }
            case "/api/transactions": {
                Result<List<Transaction>> result = context.getTransactionController()
                        .history(query.get("accountNumber"));
                if (result.isOk()) {
                    sendJson(exchange, 200,
                            JsonViews.success("OK", JsonViews.transactions(result.getData())));
                } else {
                    sendJson(exchange, 400, JsonViews.failure(result.getMessage()));
                }
                break;
            }
            case "/api/transactions/all": {
                Result<List<Transaction>> result = context.getTransactionController()
                        .allTransactions();
                if (result.isOk()) {
                    sendJson(exchange, 200,
                            JsonViews.success("OK", JsonViews.transactions(result.getData())));
                } else {
                    sendJson(exchange, 400, JsonViews.failure(result.getMessage()));
                }
                break;
            }
            case "/api/interest/run": {
                Result<List<Transaction>> result = context.getInterestController()
                        .payMonthlyInterest();
                if (result.isOk()) {
                    sendJson(exchange, 200, JsonViews.success(result.getMessage(),
                            JsonViews.transactions(result.getData())));
                } else {
                    sendJson(exchange, 400, JsonViews.failure(result.getMessage()));
                }
                break;
            }
            default:
                sendJson(exchange, 404, JsonViews.failure("Unknown endpoint: " + path));
        }
    }

    private <T> void send(Result<T> result, HttpExchange exchange, ViewMapper<T> mapper)
            throws IOException {
        if (result.isOk()) {
            Object payload = result.getData() == null ? null : mapper.map(result.getData());
            sendJson(exchange, 200, JsonViews.success(result.getMessage(), payload));
        } else {
            sendJson(exchange, 400, JsonViews.failure(result.getMessage()));
        }
    }

    private void serveStatic(HttpExchange exchange, String path) throws IOException {
        String resource;
        String contentType;
        if ("/".equals(path) || "/index.html".equals(path)) {
            resource = "/static/index.html";
            contentType = "text/html; charset=utf-8";
        } else if ("/app.css".equals(path)) {
            resource = "/static/app.css";
            contentType = "text/css; charset=utf-8";
        } else if ("/app.js".equals(path)) {
            resource = "/static/app.js";
            contentType = "application/javascript; charset=utf-8";
        } else {
            byte[] missing = "Not found".getBytes(StandardCharsets.UTF_8);
            sendBytes(exchange, 404, "text/plain; charset=utf-8", missing);
            return;
        }
        try (InputStream in = Router.class.getResourceAsStream(resource)) {
            if (in == null) {
                sendJson(exchange, 404, JsonViews.failure("Missing resource " + resource));
                return;
            }
            sendBytes(exchange, 200, contentType, in.readAllBytes());
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        sendBytes(exchange, status, "application/json; charset=utf-8", bytes);
    }

    private void sendBytes(HttpExchange exchange, int status, String contentType, byte[] bytes)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private JsonObject body(HttpExchange exchange) throws IOException {
        String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (raw.isBlank()) {
            return new JsonObject();
        }
        try {
            return JsonParser.parseString(raw).getAsJsonObject();
        } catch (RuntimeException ex) {
            return new JsonObject();
        }
    }

    private static String str(JsonObject body, String key) {
        if (body == null || !body.has(key) || body.get(key).isJsonNull()) {
            return null;
        }
        String value = body.get(key).getAsString();
        return value.isBlank() ? null : value;
    }

    private static boolean bool(JsonObject body, String key) {
        return body != null && body.has(key) && !body.get(key).isJsonNull()
                && body.get(key).getAsBoolean();
    }

    private static Long longFrom(String value) {
        try {
            return value == null ? null : Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static AccountType parseType(String value) {
        try {
            return value == null ? null : AccountType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                out.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return out;
    }

    @FunctionalInterface
    private interface ViewMapper<T> {
        Object map(T value);
    }
}
