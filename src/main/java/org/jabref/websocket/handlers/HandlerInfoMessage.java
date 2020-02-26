package org.jabref.websocket.handlers;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * possible values for messageType: info, warning, alert, error
 */
public class HandlerInfoMessage {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerInfoMessage.class);

    public static void handler(WebSocket websocket, JsonObject messagePayload) {
        String messageType = messagePayload.get("messageType").getAsString();
        String message = messagePayload.get("message").getAsString();

        LOGGER.info("[ws] " + messageType + ": " + message);
    }
}
