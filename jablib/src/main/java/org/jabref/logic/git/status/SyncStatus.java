package org.jabref.logic.git.status;

public enum SyncStatus {
    UP_TO_DATE,      // Local and remote are in sync
    BEHIND,          // Local is behind remote, pull needed
    AHEAD,           // Local is ahead of remote, push needed
    DIVERGED,        // Both local and remote have new commits; merge required
    CONFLICT,        // Merge conflict detected
    UNTRACKED,       // Not under Git control
    UNKNOWN          // Status couldn't be determined
}
