package edu.bac.banking.boundary.fx;

import edu.bac.banking.controller.BankContext;
import edu.bac.banking.controller.Result;
import edu.bac.banking.model.Account;
import edu.bac.banking.model.AccountType;
import edu.bac.banking.model.Customer;
import edu.bac.banking.model.Transaction;
import edu.bac.banking.util.Money;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Boundary class: the Meridian Bank desktop dashboard.
 *
 * Layout is organised as a brand bar, a left navigation rail and a set of
 * separate views (Overview, Accounts, Move money, Open account, Interest,
 * Statements), matching the Codespaces web UI. Every operation is delegated to
 * a controller; this class contains no business rules.
 */
public class DashboardView {

    private final BorderPane root = new BorderPane();
    private final BankContext context;
    private final Customer customer;

    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final Map<String, VBox> views = new LinkedHashMap<>();
    private final StackPane viewHost = new StackPane();

    private final Label totalBalance = new Label("BWP 0.00");
    private final Label accountCount = new Label("No accounts yet");
    private final VBox overviewAccounts = new VBox(8);
    private final ListView<Account> accountList = new ListView<>();
    private final GridPane detailGrid = new GridPane();
    private final ComboBox<Account> actionAccount = new ComboBox<>();
    private final ComboBox<Account> historyAccount = new ComboBox<>();
    private final ComboBox<AccountType> typeBox = new ComboBox<>();
    private final TextField amountField = new TextField();
    private final TextField noteField = new TextField();
    private final TextField openingField = new TextField("500.00");
    private final TextField branchField = new TextField("Gaborone Main");
    private final TextField companyField = new TextField();
    private final TextField companyAddressField = new TextField();
    private final TextArea historyArea = new TextArea();
    private final Label status = new Label(" ");

    public DashboardView(BankContext context, Customer customer, Runnable onLogout) {
        this.context = context;
        this.customer = customer;

        root.getStyleClass().add("app-root");
        root.setTop(brandBar(customer.getFullName(), onLogout));
        root.setLeft(navRail());
        root.setCenter(scrollPane());
        root.setBottom(status);
        status.getStyleClass().addAll("status-label", "app-status");

        initControls();
        buildViews();
        setView("Overview");
        refresh();
    }

    /* ---------------- controls ---------------- */

    private void initControls() {
        StringConverter<Account> label = new StringConverter<>() {
            @Override
            public String toString(Account account) {
                return account == null ? ""
                        : account.getAccountNumber() + " - " + account.getDisplayName();
            }

            @Override
            public Account fromString(String value) {
                return null;
            }
        };
        actionAccount.setConverter(label);
        historyAccount.setConverter(label);
        historyAccount.valueProperty().addListener((obs, oldValue, newValue) -> loadHistory());

        typeBox.setItems(FXCollections.observableArrayList(AccountType.values()));
        typeBox.getSelectionModel().selectFirst();

        accountList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                setText(empty || account == null ? null : account.toString());
            }
        });
        accountList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> renderAccountDetail());

        detailGrid.getStyleClass().add("detail-grid");
    }

    /* ---------------- shell ---------------- */

    private HBox brandBar(String userName, Runnable onLogout) {
        Label mark = new Label("M");
        mark.getStyleClass().add("brand-mark");
        Label name = new Label("MERIDIAN BANK");
        name.getStyleClass().add("brand-name");
        Label sub = new Label("Personal banking");
        sub.getStyleClass().add("brand-sub");

        Label user = new Label("Signed in as  " + userName);
        user.getStyleClass().add("brand-sub");

        Button refresh = ghostButton("Refresh");
        refresh.setOnAction(event -> refresh());
        Button logout = ghostButton("Log out");
        logout.setOnAction(event -> onLogout.run());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(12, mark, new VBox(1, name, sub), spacer, user, refresh, logout);
        bar.getStyleClass().add("brand-bar");
        return bar;
    }

    private VBox navRail() {
        VBox rail = new VBox(6);
        rail.getStyleClass().add("nav-rail");
        Label caption = new Label("MENU");
        caption.getStyleClass().add("nav-caption");
        rail.getChildren().add(caption);

        for (String view : new String[]{"Overview", "Accounts", "Move money",
                "Open account", "Interest", "Statements"}) {
            Button button = new Button(view);
            button.getStyleClass().add("nav-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> setView(view));
            navButtons.put(view, button);
            rail.getChildren().add(button);
        }
        return rail;
    }

    private ScrollPane scrollPane() {
        viewHost.getStyleClass().add("view-host");
        ScrollPane scroll = new ScrollPane(viewHost);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.getStyleClass().add("app-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private void setView(String key) {
        for (Map.Entry<String, VBox> entry : views.entrySet()) {
            boolean active = entry.getKey().equals(key);
            entry.getValue().setVisible(active);
            entry.getValue().setManaged(active);
        }
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(key);
            entry.getValue().getStyleClass().removeAll("nav-button-active");
            if (active) {
                entry.getValue().getStyleClass().add("nav-button-active");
            }
        }
        viewHost.requestLayout();
    }

    /* ---------------- views ---------------- */

    private void buildViews() {
        views.put("Overview", view(
                heading("Account overview", "Welcome back, " + customer.getFirstName()
                        + ". Here is your money at a glance."),
                heroCard(),
                section("Quick actions", quickActions()),
                section("Your accounts", overviewAccounts)));

        views.put("Accounts", view(
                heading("Accounts", "All accounts held under your profile."),
                section("Account list", accountList),
                section("Account details", detailGrid)));

        views.put("Move money", view(
                heading("Move money", "Deposit or withdraw funds on a selected account."),
                section("Transaction details", moveMoneyForm()),
                infoCard("Savings accounts accept deposits only. Withdrawals are available "
                        + "on investment and cheque accounts, and a balance can never go below zero.")));

        views.put("Open account", view(
                heading("Open an account", "Choose the account that fits how you plan to use your money."),
                section("New account details", openAccountForm()),
                infoCard("Savings: 0.05% monthly, deposits only.  "
                        + "Investment: 5% monthly, minimum BWP 500.00 opening deposit.  "
                        + "Cheque: salary account for employed customers, no interest.")));

        views.put("Interest", view(
                heading("Monthly interest", "Interest is paid only to accounts that earn it."),
                section("Run the interest cycle", interestPanel()),
                infoCard("The controller walks every account and pays 0.05% to savings and 5% "
                        + "to investment accounts. Cheque accounts are skipped automatically.")));

        views.put("Statements", view(
                heading("Statements", "A complete record of money moving in and out of an account."),
                section("Transaction history", statementsPanel())));

        for (VBox view : views.values()) {
            viewHost.getChildren().add(view);
        }
    }

    private VBox view(Node... children) {
        VBox box = new VBox(14);
        box.getStyleClass().add("body-wrap");
        box.getChildren().addAll(children);
        return box;
    }

    private VBox heading(String title, String subtitle) {
        Label h = new Label(title);
        h.getStyleClass().add("page-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("page-sub");
        return new VBox(4, h, s);
    }

    private HBox heroCard() {
        Label caption = new Label("TOTAL BALANCE ACROSS ALL ACCOUNTS");
        caption.getStyleClass().add("hero-caption");
        totalBalance.getStyleClass().add("balance-amount");
        accountCount.getStyleClass().add("hero-note-label");
        VBox left = new VBox(6, caption, totalBalance, accountCount);
        Label note = new Label("Deposits, withdrawals, new accounts and monthly interest\n"
                + "are available from the menu on the left.");
        note.getStyleClass().add("hero-note");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox hero = new HBox(16, left, spacer, note);
        hero.getStyleClass().add("hero-card");
        hero.setAlignment(Pos.CENTER_LEFT);
        return hero;
    }

    private GridPane quickActions() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("quick-grid");
        Button deposit = primaryButton("Deposit");
        deposit.setOnAction(event -> {
            setView("Move money");
            report("Choose an account, enter an amount and confirm the deposit.", false);
        });
        Button withdraw = ghostButton("Withdraw");
        withdraw.setOnAction(event -> {
            setView("Move money");
            report("Choose an investment or cheque account to withdraw from.", false);
        });
        Button open = ghostButton("Open account");
        open.setOnAction(event -> setView("Open account"));
        Button interest = ghostButton("Monthly interest");
        interest.setOnAction(event -> setView("Interest"));
        grid.addRow(0, deposit, withdraw);
        grid.addRow(1, open, interest);
        return grid;
    }

    private GridPane moveMoneyForm() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid-2");
        grid.addRow(0, fieldLabel("Account"), actionAccount);
        grid.addRow(1, fieldLabel("Amount (BWP)"), amountField);
        grid.addRow(2, fieldLabel("Reference"), noteField);
        amountField.setPrefWidth(220);
        noteField.setPrefWidth(220);
        Button deposit = primaryButton("Deposit");
        deposit.setOnAction(event -> transact(true));
        Button withdraw = secondaryButton("Withdraw");
        withdraw.setOnAction(event -> transact(false));
        HBox buttons = new HBox(10, deposit, withdraw);
        grid.add(buttons, 1, 3);
        return grid;
    }

    private GridPane openAccountForm() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid-2");
        grid.addRow(0, fieldLabel("Account type"), typeBox);
        grid.addRow(1, fieldLabel("Opening balance (BWP)"), openingField);
        grid.addRow(2, fieldLabel("Branch"), branchField);
        grid.addRow(3, fieldLabel("Company (cheque only)"), companyField);
        grid.addRow(4, fieldLabel("Company address"), companyAddressField);
        Button open = primaryButton("Open this account");
        open.setOnAction(event -> openAccount());
        grid.add(open, 1, 5);
        return grid;
    }

    private VBox interestPanel() {
        Label rates = new Label("Investment   5% / month       Savings   0.05% / month       Cheque   0%");
        rates.getStyleClass().add("section-title");
        Button run = secondaryButton("Pay monthly interest");
        run.setOnAction(event -> {
            Result<List<Transaction>> result = context.getInterestController().payMonthlyInterest();
            report(result.getMessage(), !result.isOk());
            refresh();
        });
        return new VBox(14, rates, run);
    }

    private VBox statementsPanel() {
        historyArea.setEditable(false);
        historyArea.setPrefRowCount(12);
        historyArea.getStyleClass().add("history-area");
        HBox filter = new HBox(10, fieldLabel("Account"), historyAccount);
        filter.setAlignment(Pos.CENTER_LEFT);
        return new VBox(12, filter, historyArea);
    }

    private VBox section(String heading, Node content) {
        Label label = new Label(heading);
        label.getStyleClass().add("section-title");
        VBox box = new VBox(10, label, content);
        box.getStyleClass().add("panel-card");
        return box;
    }

    private VBox infoCard(String text) {
        Label body = new Label(text);
        body.getStyleClass().add("muted-label");
        body.setWrapText(true);
        VBox box = new VBox(body);
        box.getStyleClass().add("info-card");
        return box;
    }

    private Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("button", "primary-button");
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("button", "secondary-button");
        return button;
    }

    private Button ghostButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("button", "ghost-button", "small-button");
        return button;
    }

    /* ---------------- controller delegation ---------------- */

    private void transact(boolean isDeposit) {
        Account account = actionAccount.getValue();
        if (account == null) {
            report("Select an account first.", true);
            return;
        }
        Result<Transaction> result = isDeposit
                ? context.getAccountController().deposit(
                        account.getAccountNumber(), amountField.getText(), noteField.getText())
                : context.getAccountController().withdraw(
                        account.getAccountNumber(), amountField.getText(), noteField.getText());
        if (result.isOk()) {
            report(result.getMessage() + " New balance: "
                    + Money.format(result.getData().getBalanceAfter()), false);
            amountField.clear();
            noteField.clear();
            refresh();
        } else {
            report(result.getMessage(), true);
        }
    }

    private void openAccount() {
        AccountType type = typeBox.getValue();
        if (type == null) {
            type = AccountType.SAVINGS;
        }
        Result<Account> result = context.getAccountController().openAccount(
                customer.getId(), type, openingField.getText(), branchField.getText(),
                companyField.getText(), companyAddressField.getText());
        report(result.getMessage(), !result.isOk());
        if (result.isOk()) {
            refresh();
        }
    }

    private void refresh() {
        Result<List<Account>> result = context.getAccountController().getAccounts(customer.getId());
        if (!result.isOk()) {
            report(result.getMessage(), true);
            return;
        }
        List<Account> accounts = result.getData();

        accountList.setItems(FXCollections.observableArrayList(accounts));
        accountList.setPrefHeight(Math.max(120, Math.min(240, accounts.size() * 34 + 20)));
        restore(actionAccount, accounts);
        restore(historyAccount, accounts);

        BigDecimal total = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalBalance.setText(Money.format(total));
        accountCount.setText(accounts.size() + (accounts.size() == 1
                ? " active account" : " active accounts"));

        List<Node> summary = new ArrayList<>();
        if (accounts.isEmpty()) {
            summary.add(new Label("No accounts yet. Open one from the menu."));
        } else {
            for (Account account : accounts) {
                summary.add(summaryRow(account));
            }
        }
        overviewAccounts.getChildren().setAll(summary);

        if (accountList.getSelectionModel().getSelectedItem() == null && !accounts.isEmpty()) {
            accountList.getSelectionModel().selectFirst();
        }
        renderAccountDetail();
        loadHistory();
    }

    private HBox summaryRow(Account account) {
        Label name = new Label(account.getDisplayName() + "   " + account.getAccountNumber());
        name.getStyleClass().add("section-title");
        Label balance = new Label(Money.format(account.getBalance()));
        balance.getStyleClass().add("section-title");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, name, spacer, balance);
        row.getStyleClass().add("summary-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void restore(ComboBox<Account> combo, List<Account> accounts) {
        String previous = combo.getValue() == null ? null : combo.getValue().getAccountNumber();
        combo.setItems(FXCollections.observableArrayList(accounts));
        if (previous != null) {
            accounts.stream()
                    .filter(a -> a.getAccountNumber().equals(previous))
                    .findFirst()
                    .ifPresent(combo::setValue);
        }
        if (combo.getValue() == null && !accounts.isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
    }

    private void renderAccountDetail() {
        detailGrid.getChildren().clear();
        Account account = accountList.getSelectionModel().getSelectedItem();
        if (account == null && actionAccount.getValue() != null) {
            account = actionAccount.getValue();
        }
        if (account == null) {
            detailGrid.addRow(0, new Label("Select an account in the list above."));
            return;
        }
        String[][] rows = {
                {"Account number", account.getAccountNumber()},
                {"Account type", account.getDisplayName()},
                {"Branch", account.getBranch()},
                {"Available balance", Money.format(account.getBalance())},
                {"Interest rate", account.getMonthlyInterestRate()
                        .multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "% / month"},
                {"Withdrawals", account.allowsWithdrawals() ? "Allowed" : "Not permitted"},
                {"Employer", account.getEmploymentInfo() == null
                        ? "Not applicable" : account.getEmploymentInfo().toString()}
        };
        for (int i = 0; i < rows.length; i++) {
            Label key = new Label(rows[i][0]);
            key.getStyleClass().add("field-label");
            Label value = new Label(rows[i][1]);
            value.getStyleClass().add("section-title");
            detailGrid.addRow(i, key, value);
        }
    }

    private void loadHistory() {
        Account account = historyAccount.getValue();
        if (account == null) {
            historyArea.setText("Select an account to see its history.");
            return;
        }
        Result<List<Transaction>> result = context.getTransactionController()
                .history(account.getAccountNumber());
        if (!result.isOk()) {
            historyArea.setText(result.getMessage());
            return;
        }
        historyArea.setText(result.getData().isEmpty()
                ? "No transactions yet."
                : result.getData().stream().map(Transaction::toString)
                        .collect(Collectors.joining(System.lineSeparator())));
    }

    private void report(String message, boolean error) {
        status.setText(message);
        status.getStyleClass().removeAll("status-ok", "status-error");
        status.getStyleClass().add(error ? "status-error" : "status-ok");
    }

    public Parent getRoot() {
        return root;
    }
}
