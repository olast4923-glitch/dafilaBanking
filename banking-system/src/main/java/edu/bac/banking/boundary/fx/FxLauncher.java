package edu.bac.banking.boundary.fx;

import javafx.application.Application;

/**
 * IntelliJ entry point.
 * A launcher that does not extend Application avoids the classic
 * "JavaFX runtime components are missing" error when running from the classpath.
 *
 *   mvn exec:java -Dexec.mainClass=edu.bac.banking.boundary.fx.FxLauncher
 */
public final class FxLauncher {

    private FxLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(FxApp.class, args);
    }
}
