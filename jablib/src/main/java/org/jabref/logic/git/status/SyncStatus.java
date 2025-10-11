package org.jabref.logic.git.status;

public enum SyncStatus {
    UP_TO_DATE,
    BEHIND,
    AHEAD,
    DIVERGED,
    CONFLICT,
    UNTRACKED,
    UNKNOWN,
    REMOTE_EMPTY
}
