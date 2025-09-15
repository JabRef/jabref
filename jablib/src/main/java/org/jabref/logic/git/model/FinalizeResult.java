package org.jabref.logic.git.model;

import java.util.Optional;

///  这是记账层({@link org.jabref.logic.git.merge.MergeBookkeeper})的返回值，表示 “我们是否做了 fast-forward，或者新写了一个（merge 或 single-parent）commit”
///  它不参与任何内容合成；输入文件内容必须是 GUI 之前已经写好的合并结果。
///  如果“本地严格落后远端且文件内容与远端完全一致”→ 快进（fast-forward），不产生新提交；
///  否则：
///  - BEHIND（local 是 remote 的祖先）：在 remote 之上新建单亲提交（parent=remote）；
///  - DIVERGED：新建双亲 merge 提交（parents=[local, remote]）。
///  Notes: 因为 UP_TO_DATE / AHEAD / CONFLICT / UNTRACKED 这些状态在 prepareMerge 前已经被 GitStatusChecker 拦掉，不会进入 finalizeMerge。
public final class FinalizeResult {
    public enum Kind {
        NOOP_UP_TO_DATE, // nothing to do (local == remote)
        NOOP_AHEAD,      // nothing to do (local ahead of remote)
        FAST_FORWARD,    // bookkeeping moved ref to remote; no new commit
        NEW_COMMIT       // bookkeeping wrote a new commit (single-parent or merge)
    }

    private final Kind kind;
    private final Optional<String> commitId;

    private FinalizeResult(Kind kind, Optional<String> commitId) {
        this.kind = kind;
        this.commitId = commitId;
    }

    public static FinalizeResult upToDate() {
        return new FinalizeResult(Kind.NOOP_UP_TO_DATE, Optional.empty());
    }

    public static FinalizeResult ahead() {
        return new FinalizeResult(Kind.NOOP_AHEAD, Optional.empty());
    }

    public static FinalizeResult fastForward() {
        return new FinalizeResult(Kind.FAST_FORWARD, Optional.empty());
    }

    public static FinalizeResult newCommit(String commitId) {
        return new FinalizeResult(Kind.NEW_COMMIT, Optional.ofNullable(commitId));
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
