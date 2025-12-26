package org.jabref.http.server.services;

import java.io.IOException;
import java.net.InetSocketAddress;

// 1. We import the Worker we just hired
import org.jabref.http.server.resources.CheckExistenceResource;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The "Manager" class.
 * Responsibilities:
 * 1. Start the HTTP Server on Port 6051.
 * 2. Assign the CheckExistenceResource to handle requests.
 */
public class CockpitServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CockpitServer.class);
    private static final int PORT = 6051;
    private HttpServer server;

    public void start() {
        try {
            // 1. Open the doors (Bind to localhost:6051)
            server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);

            // 2. Assign the Worker (The Resource) to the Desk (The Endpoint)
            // This is the clean "Integration" Oliver wanted.
            server.createContext("/check-existence", new CheckExistenceResource());

            // 3. Simple Ping (We can keep this simple for now)
            server.createContext("/ping", exchange -> {
                String resp = "{\"status\": \"connected\"}";
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, resp.length());
                exchange.getResponseBody().write(resp.getBytes());
                exchange.close();
            });

            server.setExecutor(null); // Default executor
            server.start();
            LOGGER.info("Researcher Cockpit Server started on port " + PORT);

        } catch (IOException e) {
            LOGGER.error("Failed to start Researcher Cockpit Server", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            LOGGER.info("Researcher Cockpit Server stopped.");
        }
    }
}
