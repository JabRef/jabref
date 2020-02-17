package org.jabref.websocket;

import com.google.gson.JsonObject;

public class WsServerUtils {
    public static JsonObject createMessageContainer(WsAction wsAction, JsonObject messagePayload) {
        JsonObject messageContainer = new JsonObject();
        messageContainer.addProperty("action", wsAction.toString());
        messageContainer.add("payload", messagePayload);

        return messageContainer;
    }
}
