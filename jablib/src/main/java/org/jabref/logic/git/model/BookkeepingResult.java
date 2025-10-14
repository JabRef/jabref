package org.jabref.logic.git.model;

import java.util.Optional;

/// This is the return value of the bookkeeping layer, {@link org.jabref.logic.git.merge.execution.MergeBookkeeper}
/// indicating "whether we performed a fast-forward or created a new (merge or single-parent) commit."
/// It does not participate in any content synthesis; the input file content must be the merge result already prepared before the GUI.
/// If "the local is strictly behind the remote and the file content is exactly the same as the remote" → fast-forward, no new commit is created;
/// Otherwise:
/// - BEHIND (local is an ancestor of remote): create a new single-parent commit (parent=remote) on top of remote;
/// - DIVERGED: create a new dual-parent merge commit (parents=[local, remote]).
/// Notes: Because the statuses UP_TO_DATE / AHEAD / CONFLICT / UNTRACKED are already filtered out before prepareMerge by GitStatusChecker, they will not enter finalizeMerge.
public final class BookkeepingResult {
    public enum Kind {
        NOOP_UP_TO_DATE, // nothing to do (local == remote)
        NOOP_AHEAD,      // nothing to do (local ahead of remote)
        FAST_FORWARD,    // bookkeeping moved ref to remote; no new commit
        NEW_COMMIT       // bookkeeping wrote a new commit (single-parent or merge)
    }

    private final Kind kind;
    private final Optional<String> commitId;

    private BookkeepingResult(Kind kind, Optional<String> commitId) {
        this.kind = kind;
        this.commitId = commitId;
    }

    public static BookkeepingResult upToDate() {
        return new BookkeepingResult(Kind.NOOP_UP_TO_DATE, Optional.empty());
    }

    public static BookkeepingResult ahead() {
        return new BookkeepingResult(Kind.NOOP_AHEAD, Optional.empty());
    }

    public static BookkeepingResult fastForward() {
        return new BookkeepingResult(Kind.FAST_FORWARD, Optional.empty());
    }

    public static BookkeepingResult newCommit(String commitId) {
        return new BookkeepingResult(Kind.NEW_COMMIT, Optional.ofNullable(commitId));
    }

    public Kind kind() {
        return kind;
    }

    public Optional<String> commitId() {
        return commitId;
    }

    public boolean isNoop() {
        return kind == Kind.NOOP_UP_TO_DATE || kind == Kind.NOOP_AHEAD;
    }

    public boolean isUpToDate() {
        return kind == Kind.NOOP_UP_TO_DATE;
    }

    public boolean isAhead() {
        return kind == Kind.NOOP_AHEAD;
    }

    public boolean isFastForward() {
        return kind == Kind.FAST_FORWARD;
    }

    public boolean hasNewCommit() {
        return kind == Kind.NEW_COMMIT;
    }
}
