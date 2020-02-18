package org.jabref.websocket.handlers;

import org.jabref.websocket.JabRefWebsocketServerInstance;
import org.jabref.websocket.WsAction;
import org.jabref.websocket.WsClientData;
import org.jabref.websocket.WsClientType;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;

public class HandlerCmdRegister {
    public static void handler(WebSocket websocket, JsonObject messagePayload) {
        String wsClientTypeString = messagePayload.get("wsClientType").getAsString();

        if (WsClientType.isValidWsClientType(wsClientTypeString)) {
            WsClientType wsClientType = WsClientType.getClientTypeFromString(wsClientTypeString);
            websocket.<WsClientData>getAttachment().setWsClientType(wsClientType);
        } else {
            System.out.println("[ws] invalid WsClientType: " + wsClientTypeString);

            JsonObject messagePayloadForClient = new JsonObject();
            messagePayloadForClient.addProperty("messageType", "warning");
            messagePayloadForClient.addProperty("message", "invalid WsClientType: " + wsClientTypeString);

            JabRefWebsocketServerInstance.getInstance().sendMessage(websocket, WsAction.INFO_MESSAGE, messagePayloadForClient);
        }
    }
}
