package org.jabref.http.server.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.jabref.http.dto.CheckExistenceRequest;
import org.jabref.http.dto.CheckExistenceResponse;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It reads the JSON, checks the logic, and sends back JSON.
 */
public class CheckExistenceResource implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckExistenceResource.class);
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 1. Setup Permissions (CORS) - So the browser can talk to us
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        // If the browser is just asking "Are you allowed?", say Yes (204 OK)
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // We only accept POST (sending data)
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // 405 = Method Not Allowed
            return;
        }

        try {
            // We read the raw bytes and turn them into our Java Class
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            CheckExistenceRequest request = gson.fromJson(requestBody, CheckExistenceRequest.class);

            LOGGER.info("Browser checking URL: " + request.getUrl());

            // 3. Do the Work
            // TODO: In Ticket #3, we will connect this to the real database.
            // For now, we just reply "False"
            boolean exists = false;
            String msg = "Received check for: " + request.getUrl();

            // 4. Send the Reply
            // We turn our Java Reply back into JSON text
            CheckExistenceResponse responseObj = new CheckExistenceResponse(exists, msg);
            String responseJson = gson.toJson(responseObj);

            // Write it to the output stream
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }

        } catch (Exception e) {
            // "Server Error" (500)
            LOGGER.error("Error processing request", e);
            exchange.sendResponseHeaders(500, -1);
        }
    }
}
