package org.jabref.websocket;

import com.google.gson.JsonObject;

public class WebSocketServerUtils {
    public static JsonObject createMessageContainer(WebSocketAction webSocketAction, JsonObject messagePayload) {
        JsonObject messageContainer = new JsonObject();
        messageContainer.addProperty("action", webSocketAction.toString());
        messageContainer.add("payload", messagePayload);

        return messageContainer;
    }
}
