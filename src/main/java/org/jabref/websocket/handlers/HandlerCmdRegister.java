package org.jabref.websocket.handlers;

import org.jabref.websocket.JabRefWebsocketServer;
import org.jabref.websocket.WsClientType;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;

public class HandlerCmdRegister {
    public static void handler(WebSocket websocket, JsonObject messagePayload, JabRefWebsocketServer jabRefWebsocketServer) {
        String wsClientTypeString = messagePayload.get("wsClientType").getAsString();

        WsClientType wsClientType = WsClientType.getClientTypeFromString(wsClientTypeString);

        if (wsClientType == null) {
            System.out.println("[ws] invalid WsClientType: " + wsClientTypeString);
        }

        jabRefWebsocketServer.registerWsClient(wsClientType, websocket);
    }
}
