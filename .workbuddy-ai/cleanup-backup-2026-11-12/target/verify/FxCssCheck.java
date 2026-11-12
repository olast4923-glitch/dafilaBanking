import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Temporary verification: force JavaFX to parse /fx/bank.css without opening a window. */
public final class FxCssCheck {

    public static void main(String[] args) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Platform.startup(() -> {
            try {
                VBox root = new VBox();
                root.getStyleClass().addAll("app-root");
                Label title = new Label("Meridian Bank");
                title.getStyleClass().addAll("page-title", "status-label", "status-ok");
                Button button = new Button("Sign in");
                button.getStyleClass().addAll("button", "primary-button");
                root.getChildren().addAll(title, button);

                Scene scene = new Scene(root, 600, 400);
                URL css = FxCssCheck.class.getResource("/fx/bank.css");
                if (css == null) {
                    throw new IllegalStateException("/fx/bank.css missing from classpath");
                }
                scene.getStylesheets().add(css.toExternalForm());
                root.applyCss();
                root.layout();
                System.out.println("CSS_RESOLVED=" + css);
                System.out.println("BUTTON_TEXT_FILL=" + button.getTextFill());
                System.out.println("ROOT_BACKGROUND=" + root.getBackground());
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                done.countDown();
            }
        });
        if (!done.await(30, TimeUnit.SECONDS)) {
            System.err.println("Timed out waiting for JavaFX toolkit");
        }
        Platform.exit();
    }
}
