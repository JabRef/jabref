package org.jabref.logic.git.model;

public final class PushResult implements GitOperationResult {

    public enum Outcome {
        PUSHED,
        NOOP_UP_TO_DATE
    }

    private final boolean isSuccessful;
    private final boolean noop;

    private PushResult(boolean isSuccessful, boolean noop) {
        this.isSuccessful = isSuccessful;
        this.noop = noop;
    }

    public static PushResult pushed() {
        return new PushResult(true, false);
    }

    public static PushResult noopUpToDate() {
        return new PushResult(true, true);
    }

    @Override
    public Operation operation() {
        return Operation.PUSH;
    }

    @Override
    public boolean isSuccessful() {
        return isSuccessful;
    }

    @Override
    public boolean noop() {
        return noop;
    }
}
