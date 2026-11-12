package edu.bac.banking.boundary.fx;

import edu.bac.banking.controller.LoginController;
import edu.bac.banking.controller.Result;
import edu.bac.banking.model.Customer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Boundary class: builds the branded sign-in and registration screens, collects input
 * and displays controller results. It contains no validation or business rules.
 */
public class LoginView {

    private static final String DEMO_EMAIL = "dafila.philimon@example.com";
    private static final String DEMO_PASSWORD = "Passw0rd!";

    private final BorderPane root = new BorderPane();
    private final Label status = new Label(" ");
    private final LoginController controller;
    private final Consumer<Customer> onSuccess;

    private TextField loginEmail;
    private PasswordField loginPassword;

    public LoginView(LoginController controller, Consumer<Customer> onSuccess) {
        this.controller = controller;
        this.onSuccess = onSuccess;

        root.getStyleClass().add("auth-root");
        root.setTop(brandBar());
        root.setCenter(content());
        root.setBottom(status);

        status.getStyleClass().addAll("status-label", "auth-status");
    }

    /** Same navy brand bar as the web portal's sign-in screen. */
    private HBox brandBar() {
        Label mark = new Label("M");
        mark.getStyleClass().add("brand-mark");

        Label name = new Label("MERIDIAN BANK");
        name.getStyleClass().add("brand-name");
        Label sub = new Label("Personal banking");
        sub.getStyleClass().add("brand-sub");

        Label secure = new Label("Secure online banking");
        secure.getStyleClass().add("brand-sub");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(12, mark, new VBox(1, name, sub), spacer, secure);
        bar.getStyleClass().add("brand-bar");
        return bar;
    }

    private VBox content() {
        Label title = new Label("Welcome back");
        title.getStyleClass().add("auth-title");
        Label subtitle = new Label("Sign in to view your accounts and move money securely.");
        subtitle.getStyleClass().add("auth-sub");
        subtitle.setWrapText(true);

        VBox heading = new VBox(5, title, subtitle);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().add(new Tab("Sign in", loginForm()));
        tabs.getTabs().add(new Tab("Register", registerForm()));
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Label demoTitle = new Label("Demo access");
        demoTitle.getStyleClass().add("section-title");
        Label demoText = new Label("Dafila Philimon  |  " + DEMO_EMAIL + " / " + DEMO_PASSWORD);
        demoText.getStyleClass().add("muted-label");
        demoText.setWrapText(true);
        Button fillDemo = ghostButton("Use demo");
        fillDemo.setOnAction(event -> {
            loginEmail.setText(DEMO_EMAIL);
            loginPassword.setText(DEMO_PASSWORD);
            tabs.getSelectionModel().selectFirst();
        });
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox demoRow = new HBox(10, demoTitle, demoText, spacer, fillDemo);
        demoRow.setAlignment(Pos.CENTER_LEFT);
        demoRow.getStyleClass().add("demo-box");

        VBox box = new VBox(14, heading, tabs, demoRow);
        box.getStyleClass().add("auth-wrap");
        return box;
    }

    private GridPane loginForm() {
        GridPane grid = form();
        loginEmail = new TextField(DEMO_EMAIL);
        loginPassword = new PasswordField();
        loginPassword.setText(DEMO_PASSWORD);

        grid.addRow(0, fieldLabel("Email address"), loginEmail);
        grid.addRow(1, fieldLabel("Password"), loginPassword);

        Button login = primaryButton("Sign in");
        login.setOnAction(event -> {
            Result<Customer> result = controller.login(
                    loginEmail.getText(), loginPassword.getText());
            if (result.isOk()) {
                onSuccess.accept(result.getData());
            } else {
                report(result.getMessage(), true);
            }
        });
        grid.add(login, 1, 2);
        GridPane.setMargin(login, new Insets(8, 0, 0, 0));
        return grid;
    }

    private GridPane registerForm() {
        GridPane grid = form();
        TextField first = new TextField();
        TextField surname = new TextField();
        TextField address = new TextField();
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        CheckBox employed = new CheckBox("Currently employed");
        TextField company = new TextField();
        TextField companyAddress = new TextField();

        grid.addRow(0, fieldLabel("First name"), first);
        grid.addRow(1, fieldLabel("Surname"), surname);
        grid.addRow(2, fieldLabel("Address"), address);
        grid.addRow(3, fieldLabel("Email address"), email);
        grid.addRow(4, fieldLabel("Password"), password);
        grid.add(employed, 1, 5);
        grid.addRow(6, fieldLabel("Company"), company);
        grid.addRow(7, fieldLabel("Company address"), companyAddress);

        Button register = primaryButton("Create account");
        register.setOnAction(event -> {
            Result<Customer> result = controller.register(
                    first.getText(), surname.getText(), address.getText(), email.getText(),
                    password.getText(), employed.isSelected(), company.getText(),
                    companyAddress.getText());
            if (result.isOk()) {
                onSuccess.accept(result.getData());
            } else {
                report(result.getMessage(), true);
            }
        });
        grid.add(register, 1, 8);
        GridPane.setMargin(register, new Insets(8, 0, 0, 0));
        return grid;
    }

    private GridPane form() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        return grid;
    }

    private Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("button", "primary-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private Button ghostButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("button", "ghost-button", "small-button");
        return button;
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
