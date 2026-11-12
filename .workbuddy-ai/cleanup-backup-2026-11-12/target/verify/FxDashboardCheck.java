import edu.bac.banking.boundary.fx.DashboardView;
import edu.bac.banking.controller.BankContext;
import edu.bac.banking.model.Customer;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Temporary verification: build and lay out the reorganised dashboard headlessly. */
public final class FxDashboardCheck {

    public static void main(String[] args) throws Exception {
        BankContext context = BankContext.start();
        Customer customer = context.getLoginController()
                .login("dafila.philimon@example.com", "Passw0rd!").getData();

        CountDownLatch done = new CountDownLatch(1);
        Platform.startup(() -> {
            try {
                DashboardView view = new DashboardView(context, customer, () -> { });
                Scene scene = new Scene(view.getRoot(), 1020, 780);
                scene.getStylesheets().add(
                        FxDashboardCheck.class.getResource("/fx/bank.css").toExternalForm());
                Parent root = scene.getRoot();
                root.applyCss();
                root.layout();
                System.out.println("DASHBOARD_OK nodes=" + count(root));
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("DASHBOARD_FAILED");
            } finally {
                done.countDown();
            }
        });
        if (!done.await(30, TimeUnit.SECONDS)) {
            System.err.println("Timed out waiting for JavaFX toolkit");
        }
        Platform.exit();
    }

    private static int count(Parent parent) {
        int total = 1;
        for (var node : parent.getChildrenUnmodifiable()) {
            total += node instanceof Parent p ? count(p) : 1;
        }
        return total;
    }
}
