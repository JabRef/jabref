package org.jabref.websocket;

public enum WsAction {
    // send only
    HEARTBEAT("heartbeat"),
    CMD_GET_GOOGLE_SCHOLAR_CITATION_COUNTS("cmd.getGoogleScholarCitationCounts"),

    // receive only
    CMD_SUBSCRIBE("cmd.subscribe"),
    INFO_GOOGLE_SCHOLAR_CITATION_COUNTS("info.googleScholarCitationCounts"),
    INFO_GOOGLE_SCHOLAR_SOLVING_CAPTACHA_NEEDED("info.googleScholarSolvingCaptchaNeeded"),

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

    @Override
    public String toString() {
        return wsAction;
    }
}
