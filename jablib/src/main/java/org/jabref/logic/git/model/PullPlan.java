package org.jabref.logic.git.model;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.status.SyncStatus;

import org.eclipse.jgit.revwalk.RevCommit;

/// PullPlan contains:
///  base / remote / localHead commits (for finalize)
///  autoPlan: MergePlan of auto-applicable patches (no conflicts)
///  conflicts: List<ThreeWayEntryConflict> that require user resolution
public final class PullPlan {
    private final SyncStatus status;
    private final Optional<RevCommit> base;
    private final RevCommit localHead;
    private final RevCommit remote;
    private final MergePlan autoPlan;
    private final List<ThreeWayEntryConflict> conflicts;

    private PullPlan(SyncStatus status,
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

    public static PullPlan of(SyncStatus syncStatus,
                              Optional<RevCommit> base,
                              RevCommit localHead,
                              RevCommit remote,
                              MergePlan autoPlan,
                              List<ThreeWayEntryConflict> conflicts) {
        return new PullPlan(syncStatus, base, localHead, remote, autoPlan, conflicts);
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
}
