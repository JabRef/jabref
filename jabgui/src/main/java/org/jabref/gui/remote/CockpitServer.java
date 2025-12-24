package org.jabref.gui.remote;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * The "Ear" of JabRef.
 * Implements [REQ-008] Bidirectional Communication.
 * Listens on Port 6050 for the Browser Plugin.
 */
public class CockpitServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CockpitServer.class);
    private static final int PORT = 6051;
    private HttpServer server;

    public void start() {
        try {
            // 1. Create the Server on localhost
            server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);

            // 2. Define Endpoints (Register them BEFORE starting)
            server.createContext("/ping", new PingHandler());
            server.createContext("/check-existence", new CheckExistenceHandler());

            // 3. Start Listening
            server.setExecutor(null);
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

    /**
     * Handles simple connectivity checks from the browser.
     */
    static class PingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "{\"status\": \"connected\", \"message\": \"JabRef is listening!\"}";

            // CRITICAL: Allow the browser extension to talk to us (CORS)
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "application/json");

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    /**
     * Handles incoming URLs from the browser.
     * URL: POST /check-existence
     * MOVED INSIDE the class so it can access LOGGER
     */
    static class CheckExistenceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // 1. Setup CORS (So browser doesn't block us)
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            // Handle "Pre-flight" check (Browser asks: "Can I post?")
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                t.sendResponseHeaders(204, -1);
                return;
            }

            // 2. Read the Data sent from Browser
            String requestBody = new String(t.getRequestBody().readAllBytes());
            LOGGER.info("Browser sent: " + requestBody);

            // 3. TODO: Connect to Real Logic (Ticket #3)
            // For now, we mock the response
            String response = "{\"exists\": false, \"message\": \"I received your URL!\"}";

            t.getResponseHeaders().add("Content-Type", "application/json");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    } // End of CheckExistenceHandler

} // <--- This final brace closes the CockpitServer class.
