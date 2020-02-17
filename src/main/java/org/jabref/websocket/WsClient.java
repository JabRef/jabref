package org.jabref.websocket;

import org.java_websocket.WebSocket;

public class WsClient {
    private WsClientType wsClientType;
    private WebSocket websocket;

    public WsClient(WsClientType wsClientType, WebSocket websocket) throws IllegalArgumentException {
        if (websocket == null) {
            throw new IllegalArgumentException("websocket must not be null.");
        }

        this.wsClientType = wsClientType;
        this.websocket = websocket;
    }

    public WsClientType getWsClientType() {
        return wsClientType;
    }

    public void setWsClientType(WsClientType wsClientType) {
        this.wsClientType = wsClientType;
    }

    public WebSocket getWebsocket() {
        return websocket;
    }

    public void setWebsocket(WebSocket websocket) {
        this.websocket = websocket;
    }
}
