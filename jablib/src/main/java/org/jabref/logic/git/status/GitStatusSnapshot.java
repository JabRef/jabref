package org.jabref.logic.git.status;

import java.util.Optional;

public record GitStatusSnapshot(
    boolean tracking,
    SyncStatus syncStatus,
    boolean conflict,
    boolean uncommittedChanges,
    Optional<String> lastPulledCommit) { }
