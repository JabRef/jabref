package org.jabref.logic.git.model;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.status.SyncStatus;

import org.eclipse.jgit.revwalk.RevCommit;

/// PullComputation contains:
///  base / remote / localHead commits (for finalize)
///  autoPlan: MergePlan of auto-applicable patches (no conflicts)
///  conflicts: List<ThreeWayEntryConflict> that require user resolution
///  stats: MergeStats for UX visibility
public final class PullComputation {
    private final SyncStatus status;
    private final Optional<RevCommit> base;
    private final RevCommit localHead;
    private final RevCommit remote;
    private final MergePlan autoPlan;
    private final List<ThreeWayEntryConflict> conflicts;

    private PullComputation(SyncStatus status,
                            Optional<RevCommit> base,
                            RevCommit localHead,
                            RevCommit remote,
                            MergePlan autoPlan,
                            List<ThreeWayEntryConflict> conflicts) {
        this.status = status;
        this.base = base;
        this.localHead = localHead;
        this.remote = remote;
        this.autoPlan = autoPlan;
        this.conflicts = conflicts;
    }

    public static PullComputation of(SyncStatus syncStatus,
                                     Optional<RevCommit> base,
                                     RevCommit remote,
                                     RevCommit localHead,
                                     MergePlan autoPlan,
                                     List<ThreeWayEntryConflict> conflicts) {
        return new PullComputation(syncStatus, base, localHead, remote, autoPlan, conflicts);
    }

    public static PullComputation noop() {
        return new PullComputation(SyncStatus.UP_TO_DATE, Optional.empty(), null, null, MergePlan.empty(), List.of());
    }

    public static PullComputation noopAhead() {
        return new PullComputation(SyncStatus.AHEAD, Optional.empty(), null, null, MergePlan.empty(), List.of());
    }

    public SyncStatus status() {
        return status;
    }

    public Optional<RevCommit> base() {
        return base;
    }

    public RevCommit localHead() {
        return localHead;
    }

    public RevCommit remote() {
        return remote;
    }

    public MergePlan autoPlan() {
        return autoPlan;
    }

    public List<ThreeWayEntryConflict> conflicts() {
        return conflicts;
    }

    public boolean isNoop() {
        return status == SyncStatus.UP_TO_DATE;
    }

    public boolean isNoopAhead() {
        return status == SyncStatus.AHEAD;
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }

    public boolean hasAutoChanges() {
        return autoPlan != null && !autoPlan.isEmpty();
    }
}
