package org.jabref.http.server.ws;

import org.jabref.logic.remote.server.RemoteMessageHandler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.hk2.api.ServiceLocator;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefWebSocketApp extends WebSocketApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefWebSocketApp.class);

    private final ServiceLocator serviceLocator;

    private RemoteMessageHandler handler;

    public JabRefWebSocketApp(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public void onConnect(WebSocket socket) {
        LOGGER.debug("WebSocket connected: {}", socket);
        if (handler == null) {
            handler = serviceLocator.getService(RemoteMessageHandler.class);
            if (handler == null) {
                LOGGER.warn("RemoteMessageHandler not available in ServiceLocator");
            }
        }
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
        LOGGER.debug("WS received text: {}", text);

        // Always fetch handler from ServiceLocator for each message
        RemoteMessageHandler currentHandler = serviceLocator.getService(RemoteMessageHandler.class);
        if (currentHandler == null) {
            LOGGER.warn("RemoteMessageHandler not available in ServiceLocator");
        }

        String response = handleTextMessage(text, currentHandler);
        if (response != null) {
            socket.send(response);
        }
    }

    @Override
    public void onClose(WebSocket socket, DataFrame frame) {
        LOGGER.debug("WebSocket closed: {}", socket);
    }

    @Override
    public boolean onError(WebSocket socket, Throwable error) {
        LOGGER.error("WebSocket error", error);
        return true;
    }

    String handleTextMessage(String message, RemoteMessageHandler handler) {
        try {
            String command = extractJsonValue(message, "command");
            String argument = extractJsonValue(message, "argument");

            if (command == null) {
                LOGGER.debug("No command found in WebSocket message");
                return "{\"status\":\"error\",\"message\":\"Command not specified\"}";
            }

            LOGGER.debug("Processing command: {} with argument: {}", command, argument);

            switch (command) {
                case "ping":
                    return "{\"status\":\"success\",\"response\":\"pong\"}";
                case "focus":
                    if (handler != null) {
                        handler.handleFocus();
                    }
                    return "{\"status\":\"success\",\"response\":\"focused\"}";
                case "open":
                    if (argument != null && !argument.isEmpty() && handler != null) {
                        handler.handleCommandLineArguments(new String[]{"--import", argument});
                        return "{\"status\":\"success\",\"response\":\"opened\"}";
                    }
                    return "{\"status\":\"error\",\"message\":\"No file specified\"}";
                case "add":
                    if (argument != null && !argument.isEmpty() && handler != null) {
                        handler.handleCommandLineArguments(new String[]{"--importBibtex", argument});
                        return "{\"status\":\"success\",\"response\":\"added\"}";
                    }
                    return "{\"status\":\"error\",\"message\":\"No BibTeX entry specified.\"}";
                default:
                    return "{\"status\":\"error\",\"message\":\"Unknown command: " + command + "\"}";
            }
        } catch (Exception e) {
            LOGGER.error("Error processing WebSocket message", e);
            return "{\"status\":\"error\",\"message\":\"Internal error\"}";
        }
    }

    private @Nullable String extractJsonValue(String json, String key) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject obj = parsed.getAsJsonObject();
            if (!obj.has(key) || obj.get(key).isJsonNull()) {
                return null;
            }
            return obj.get(key).getAsString();
        } catch (Exception e) {
            LOGGER.debug("Failed to parse JSON in WebSocket message: {}", e.toString());
            return null;
        }
    }
}
