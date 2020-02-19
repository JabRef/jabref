package org.jabref.websocket;

public enum WsAction {
    // send only
    HEARTBEAT("heartbeat"),
    INFO_CONFIGURATION("info.configuration"),
    CMD_FETCH_GOOGLE_SCHOLAR_CITATION_COUNTS("cmd.fetchGoogleScholarCitationCounts"),
    CMD_CONTINUE_FETCH_GOOGLE_SCHOLAR_CITATION_COUNTS("cmd.continueFetchGoogleScholarCitationCounts"),

    // receive only
    CMD_REGISTER("cmd.register"),
    INFO_GOOGLE_SCHOLAR_CITATION_COUNTS("info.googleScholarCitationCounts"),
    INFO_FETCH_GOOGLE_SCHOLAR_CITATION_COUNTS_INTERRUPTED("info.fetchGoogleScholarCitationCountsInterrupted"),

    // send and receive
    INFO_MESSAGE("info.message");

    private String wsAction;

    WsAction(String wsAction) {
        this.wsAction = wsAction;
    }

    public static boolean isValidWsAction(String wsAction) {
        for (WsAction lWsAction : WsAction.values()) {
            if (lWsAction.toString().equals(wsAction)) {
                return true;
            }
        }

        return false;
    }

    public static WsAction getWsActionFromString(String wsAction) {
        for (WsAction lWsAction : WsAction.values()) {
            if (lWsAction.toString().equals(wsAction)) {
                return lWsAction;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return wsAction;
    }
}
