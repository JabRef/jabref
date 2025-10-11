package org.jabref.logic.git.io;

import java.util.Optional;

import org.eclipse.jgit.revwalk.RevCommit;
import org.jspecify.annotations.NullMarked;

/**
 * Holds the three relevant commits involved in a semantic three-way merge,
 * it is a helper value object used exclusively during merge resolution, not part of the domain model
 *
 * @param base   the merge base (common ancestor of local and remote)
 * @param local  the current local branch tip
 * @param remote the tip of the remote tracking branch (typically origin/main)
 */
@NullMarked
public record RevisionTriple(
        Optional<RevCommit> base,
        RevCommit local,
        RevCommit remote) {
    public RevisionTriple {
    }
}
