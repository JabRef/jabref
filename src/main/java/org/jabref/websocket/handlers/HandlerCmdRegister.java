package org.jabref.websocket.handlers;

import org.jabref.websocket.JabRefWebsocketServer;
import org.jabref.websocket.WsAction;
import org.jabref.websocket.WsClientData;
import org.jabref.websocket.WsClientType;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerCmdRegister {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerCmdRegister.class);

    public static void handler(WebSocket websocket, JsonObject messagePayload) {
        String wsClientTypeString = messagePayload.get("wsClientType").getAsString();

        if (WsClientType.isValidWsClientType(wsClientTypeString)) {
            WsClientType wsClientType = WsClientType.getClientTypeFromString(wsClientTypeString);
            websocket.<WsClientData>getAttachment().setWsClientType(wsClientType);
        } else {
            LOGGER.warn("[ws] invalid WsClientType: " + wsClientTypeString);

            JsonObject messagePayloadForClient = new JsonObject();
            messagePayloadForClient.addProperty("messageType", "warning");
            messagePayloadForClient.addProperty("message", "invalid WsClientType: " + wsClientTypeString);

            JabRefWebsocketServer.getInstance().sendMessage(websocket, WsAction.INFO_MESSAGE, messagePayloadForClient);
        }
    }
}
