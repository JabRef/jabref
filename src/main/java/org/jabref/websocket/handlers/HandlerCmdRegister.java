package org.jabref.websocket.handlers;

import org.jabref.websocket.JabRefWebsocketServer;
import org.jabref.websocket.WebSocketAction;
import org.jabref.websocket.WebSocketClientData;
import org.jabref.websocket.WebSocketClientType;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerCmdRegister {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerCmdRegister.class);

    public static void handler(WebSocket websocket, JsonObject messagePayload) {
        String webSocketClientTypeString = messagePayload.get("webSocketClientType").getAsString();

        if (WebSocketClientType.isValidWebSocketClientType(webSocketClientTypeString)) {
            WebSocketClientType webSocketClientType = WebSocketClientType.getClientTypeFromString(webSocketClientTypeString);
            websocket.<WebSocketClientData>getAttachment().setWebSocketClientType(webSocketClientType);
        } else {
            LOGGER.warn("[ws] invalid WebSocketClientType: " + webSocketClientTypeString);

            JsonObject messagePayloadForClient = new JsonObject();
            messagePayloadForClient.addProperty("messageType", "warning");
            messagePayloadForClient.addProperty("message", "invalid WebSocketClientType: " + webSocketClientTypeString);

            JabRefWebsocketServer.getInstance().sendMessage(websocket, WebSocketAction.INFO_MESSAGE, messagePayloadForClient);
        }
    }
}
