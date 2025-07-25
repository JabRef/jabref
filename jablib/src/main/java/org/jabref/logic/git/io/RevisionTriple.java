package org.jabref.logic.git.io;

import org.eclipse.jgit.revwalk.RevCommit;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Holds the three relevant commits involved in a semantic three-way merge,
 * it is a helper value object used exclusively during merge resolution, not part of the domain model
 *
 * @param base the merge base (common ancestor of local and remote)
 * @param local the current local branch tip
 * @param remote the tip of the remote tracking branch (typically origin/main)
 */
public record RevisionTriple(@Nullable RevCommit base,
                             @NonNull RevCommit local,
                             @NonNull RevCommit remote) {
    public RevisionTriple {
        if (local == null || remote == null) {
            throw new IllegalArgumentException("local and remote commits must not be null");
        }
    }
}
