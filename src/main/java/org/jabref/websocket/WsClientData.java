package org.jabref.websocket;

import org.apache.commons.lang3.RandomStringUtils;

public class WsClientData {
    private WsClientType wsClientType;
    private String wsUID;

    public WsClientData(WsClientType wsClientType) {
        this.wsClientType = wsClientType;
        this.wsUID = RandomStringUtils.random(20, true, true);
    }

    public WsClientType getWsClientType() {
        return wsClientType;
    }

    public void setWsClientType(WsClientType wsClientType) {
        this.wsClientType = wsClientType;
    }

    public String getWsUID() {
        return wsUID;
    }

    public void setWsUID(String wsUID) {
        this.wsUID = wsUID;
    }
}
