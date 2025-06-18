package org.jabref.logic.git.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Find the base/local/remote three commits:
 * base = merge-base of HEAD and origin/main
 * local = HEAD
 * remote = origin/main
 */
public class GitRevisionLocator {
    public RevisionTriple locateMergeCommits(Git git) throws Exception {
        // assumes the remote branch is 'origin/main'
        ObjectId headId = git.getRepository().resolve("HEAD");
        // and uses the default remote tracking reference
        // does not support multiple remotes or custom remote branch names so far
        ObjectId remoteId = git.getRepository().resolve("refs/remotes/origin/main");
        if (remoteId == null) {
            throw new IllegalStateException("Remote branch missing origin/main.");
        }

        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit local = walk.parseCommit(headId);
            RevCommit remote = walk.parseCommit(remoteId);

            walk.setRevFilter(org.eclipse.jgit.revwalk.filter.RevFilter.MERGE_BASE);
            walk.markStart(local);
            walk.markStart(remote);

            RevCommit base = walk.next();

            return new RevisionTriple(base, local, remote);
        }
    }
}
