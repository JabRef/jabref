package org.jabref.websocket.handlers;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;

public class HandlerInfoGoogleScholarCitationCounts {

    public static final Object MESSAGE_SYNC_OBJECT = new Object();

    private static JsonObject currentMessagePayload = new JsonObject();

    public static void handler(WebSocket websocket, JsonObject messagePayload) {

        synchronized (MESSAGE_SYNC_OBJECT) {
            currentMessagePayload = messagePayload;

            MESSAGE_SYNC_OBJECT.notifyAll();
        }
    }

    public static JsonObject getCurrentMessagePayload() {
        synchronized (MESSAGE_SYNC_OBJECT) {
            return currentMessagePayload;
        }
    }
}
