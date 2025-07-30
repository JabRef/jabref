package org.jabref.logic.git.status;

import java.util.Optional;

public record GitStatusSnapshot(
    boolean tracking,
    SyncStatus syncStatus,
    boolean conflict,
    boolean uncommittedChanges,
    Optional<String> lastPulledCommit) {
    public static final boolean TRACKING = true;
    public static final boolean NOT_TRACKING = false;
    public static final boolean CONFLICT = true;
    public static final boolean NO_CONFLICT = false;
    public static final boolean UNCOMMITTED = true;
    public static final boolean NO_UNCOMMITTED = false;
}
