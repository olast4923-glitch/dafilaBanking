package edu.bac.banking.boundary.fx;

import edu.bac.banking.controller.BankContext;
import edu.bac.banking.dao.Database;
import edu.bac.banking.model.Customer;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Wires the JavaFX boundary classes to the controller layer. No business logic here.
 * The shared stylesheet keeps the IntelliJ desktop UI visually consistent with the
 * Codespaces web UI (same Meridian Bank brand, palette and components).
 */
public class FxApp extends Application {

    private static final String STYLESHEET = "/fx/bank.css";

    private BankContext context;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        this.context = BankContext.start();

        primaryStage.setTitle("Meridian Bank | Personal banking - " + Database.activeTarget());
        showLogin();
        primaryStage.setMinWidth(820);
        primaryStage.setMinHeight(660);
        primaryStage.show();
    }

    private void showLogin() {
        LoginView view = new LoginView(context.getLoginController(), this::showDashboard);
        Scene scene = styledScene(view.getRoot(), 540, 600);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    private void showDashboard(Customer customer) {
        DashboardView view = new DashboardView(context, customer, this::showLogin);
        Scene scene = styledScene(view.getRoot(), 1020, 780);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    /** A resource-based stylesheet works both from IntelliJ and from a packaged JAR. */
    private Scene styledScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        var url = FxApp.class.getResource(STYLESHEET);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        } else {
            System.err.println("Warning: " + STYLESHEET + " not found on the classpath.");
        }
        return scene;
    }
}
