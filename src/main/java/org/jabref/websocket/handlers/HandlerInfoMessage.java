package org.jabref.websocket.handlers;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;

/**
 * possible values for messageType: info, warning, alert, error
 */
public class HandlerInfoMessage {
    public static void handler(WebSocket websocket, JsonObject messagePayload) {
        String messageType = messagePayload.get("messageType").getAsString();
        String message = messagePayload.get("message").getAsString();

        System.out.println("[ws] " + messageType + ": " + message);
    }
}
