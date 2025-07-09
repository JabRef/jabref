package org.jabref.logic.git.io;

import java.io.IOException;

import org.jabref.logic.JabRefException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

/**
 * Find the base/local/remote three commits:
 * base = merge-base of HEAD and origin/main
 * local = HEAD
 * remote = origin/main
 */
public class GitRevisionLocator {
    private static final String HEAD = "HEAD";
    private static final String REMOTE = "refs/remotes/origin/main";

    public RevisionTriple locateMergeCommits(Git git) throws GitAPIException, IOException, JabRefException {
        Repository repo = git.getRepository();
        // assumes the remote branch is 'origin/main'
        ObjectId headId = repo.resolve(HEAD);
        // and uses the default remote tracking reference
        // does not support multiple remotes or custom remote branch names so far
        ObjectId remoteId = repo.resolve(REMOTE);
        if (remoteId == null) {
            throw new IllegalStateException("Remote branch missing origin/main.");
        }

        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit local = walk.parseCommit(headId);
            RevCommit remote = walk.parseCommit(remoteId);
            RevCommit base = findMergeBase(repo, local, remote);

            return new RevisionTriple(base, local, remote);
        }
    }

    public static RevCommit findMergeBase(Repository repo, RevCommit a, RevCommit b) throws IOException {
        try (RevWalk walk = new RevWalk(repo)) {
            walk.setRevFilter(RevFilter.MERGE_BASE);
            walk.markStart(a);
            walk.markStart(b);
            return walk.next();
        }
    }
}
