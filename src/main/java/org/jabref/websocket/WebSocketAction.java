package org.jabref.websocket;

public enum WebSocketAction {
    // send only
    HEARTBEAT("heartbeat"),
    INFO_CONFIGURATION("info.configuration"),
    CMD_FETCH_GOOGLE_SCHOLAR_CITATION_COUNTS("cmd.fetchGoogleScholarCitationCounts"),

    // receive only
    CMD_REGISTER("cmd.register"),
    INFO_GOOGLE_SCHOLAR_CITATION_COUNTS("info.googleScholarCitationCounts"),

    // send and receive
    INFO_MESSAGE("info.message");

    private String webSocketAction;

    WebSocketAction(String webSocketAction) {
        this.webSocketAction = webSocketAction;
    }

    public static boolean isValidWebSocketAction(String webSocketAction) {
        for (WebSocketAction lWebSocketAction : WebSocketAction.values()) {
            if (lWebSocketAction.toString().equals(webSocketAction)) {
                return true;
            }
        }

        return false;
    }

    public static WebSocketAction getWebSocketActionFromString(String webSocketAction) {
        for (WebSocketAction lWebSocketAction : WebSocketAction.values()) {
            if (lWebSocketAction.toString().equals(webSocketAction)) {
                return lWebSocketAction;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return webSocketAction;
    }
}
