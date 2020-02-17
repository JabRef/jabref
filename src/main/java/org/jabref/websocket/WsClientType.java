package org.jabref.websocket;

public enum WsClientType {
    UNKNOWN("unknown"),
    JABREF_BROWSER_EXTENSION("JabRefBrowserExtension");

    private String wsClientType;

    WsClientType(String wsClientType) {
        this.wsClientType = wsClientType;
    }

    public static boolean isValidWsClientType(String wsClientType) {
        for (WsClientType lWsClientType : WsClientType.values()) {
            if (lWsClientType.toString().equals(wsClientType)) {
                return true;
            }
        }

        return false;
    }

    public static WsClientType getClientTypeFromString(String wsClientType) {
        for (WsClientType lWsClientType : WsClientType.values()) {
            if (lWsClientType.toString().equals(wsClientType)) {
                return lWsClientType;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return wsClientType;
    }
}
