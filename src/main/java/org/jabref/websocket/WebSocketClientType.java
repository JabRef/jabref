package org.jabref.websocket;

public enum WebSocketClientType {
    UNKNOWN("unknown"),
    JABREF_BROWSER_EXTENSION("JabRefBrowserExtension");

    private String webSocketClientType;

    WebSocketClientType(String webSocketClientType) {
        this.webSocketClientType = webSocketClientType;
    }

    public static boolean isValidWebSocketClientType(String webSocketClientType) {
        for (WebSocketClientType lWebSocketClientType : WebSocketClientType.values()) {
            if (lWebSocketClientType.toString().equals(webSocketClientType)) {
                return true;
            }
        }

        return false;
    }

    public static WebSocketClientType getClientTypeFromString(String webSocketClientType) {
        for (WebSocketClientType lWebSocketClientType : WebSocketClientType.values()) {
            if (lWebSocketClientType.toString().equals(webSocketClientType)) {
                return lWebSocketClientType;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return webSocketClientType;
    }
}
