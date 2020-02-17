package org.jabref.websocket;

public enum WsClientType {
    JABREF_BROWSER_EXTENSION("JabRefBrowserExtension");

    private String wsClientType;

    WsClientType(String wsClientType) {
        this.wsClientType = wsClientType;
    }

    public static boolean isValidWsClientType(String wsClientType) {
        for (WsAction lWsClientType : WsAction.values()) {
            if (lWsClientType.toString().equals(wsClientType)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return wsClientType;
    }
}
