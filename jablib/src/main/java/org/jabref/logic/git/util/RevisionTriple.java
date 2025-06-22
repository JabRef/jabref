package org.jabref.logic.git.util;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Holds the three relevant commits involved in a semantic three-way merge,
 * it is a helper value object used exclusively during merge resolution, not part of the domain model
 * so currently placed in the logic package, may be moved to model in the future
 *
 * @param base the merge base (common ancestor of local and remote)
 * @param local the current local branch tip
 * @param remote the tip of the remote tracking branch (typically origin/main)
 */
public record RevisionTriple(RevCommit base, RevCommit local, RevCommit remote) { }
