package org.jabref.logic.git.model;

public record PushResult(boolean successful, boolean noop) {
    public static PushResult pushed() {
        return new PushResult(true, false);
    }

    public static PushResult noopUpToDate() {
        return new PushResult(false, true);
    }
}
