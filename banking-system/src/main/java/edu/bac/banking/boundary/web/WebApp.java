package edu.bac.banking.boundary.web;

import com.sun.net.httpserver.HttpServer;
import edu.bac.banking.controller.BankContext;
import edu.bac.banking.dao.Database;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Codespaces entry point. Runs a plain JDK HTTP server on port 8080 and serves
 * the browser UI; GitHub Codespaces forwards that port and opens it as a preview.
 *
 *   mvn exec:java
 */
public final class WebApp {

    private WebApp() {
    }

    public static void main(String[] args) throws IOException {
        BankContext context = BankContext.start();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        // Codespaces' port-forwarding agent reaches the app from outside the container,
        // so bind to all interfaces there; local IntelliJ runs stay on loopback.
        String defaultHost = System.getenv("CODESPACES") != null ? "0.0.0.0" : "127.0.0.1";
        String host = System.getenv().getOrDefault("HOST", defaultHost);

        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", new Router(context));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Meridian Bank (CSE202 Banking System) is running at http://"
                + host + ":" + port);
        System.out.println("Database: " + Database.activeTarget());
        System.out.println("Demo login: dafila.philimon@example.com / Passw0rd!");
    }
}
