package org.jabref.websocket.handlers;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;

public class HandlerInfoGoogleScholarCitationCountsInterrupted {
    public static void handler(WebSocket websocket, JsonObject messagePayload) {
        System.out.println("[ws] Google Scholar Captcha needs to be solved.");
    }
}
