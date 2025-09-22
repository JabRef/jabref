package org.jabref.logic.git.model;

public record PushResult(boolean successful, boolean noop) {
    /// Created when local branch has commits to push and the push succeeded.
    public static PushResult pushed() {
        return new PushResult(true, false);
    }

    /// Created when there is nothing to push (local is up to date with remote).
    public static PushResult noopUpToDate() {
        return new PushResult(false, true);
    }
}
