package org.jabref.websocket;

import org.apache.commons.lang3.RandomStringUtils;

public class WebSocketClientData {
    private WebSocketClientType webSocketClientType;
    private String webSocketUID;

    public WebSocketClientData(WebSocketClientType webSocketClientType) {
        this.webSocketClientType = webSocketClientType;
        this.webSocketUID = RandomStringUtils.random(20, true, true);
    }

    public WebSocketClientType getWebSocketClientType() {
        return webSocketClientType;
    }

    public void setWebSocketClientType(WebSocketClientType webSocketClientType) {
        this.webSocketClientType = webSocketClientType;
    }

    public String getWebSocketUID() {
        return webSocketUID;
    }

    public void setWebSocketUID(String webSocketUID) {
        this.webSocketUID = webSocketUID;
    }
}
