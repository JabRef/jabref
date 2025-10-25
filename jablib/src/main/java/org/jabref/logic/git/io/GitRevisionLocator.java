package org.jabref.logic.git.io;

import java.io.IOException;
import java.util.Optional;

import org.jabref.logic.JabRefException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

///  Locates the three key commits required for a semantic merge:
/// - base: the common ancestor of local (HEAD) and remote (origin/main)
/// - local: the current working commit (HEAD)
/// - remote: the latest commit on origin/main
public class GitRevisionLocator {

    public RevisionTriple locateMergeCommits(Git git) throws GitAPIException, IOException, JabRefException {
        Repository repo = git.getRepository();

        ObjectId headId = repo.resolve("HEAD^{commit}");
        assert headId != null : "Local HEAD commit is missing.";

        // Determine current branch name
        String fullBranch = repo.getFullBranch(); // e.g. "refs/heads/main"
        if (fullBranch == null || !fullBranch.startsWith(Constants.R_HEADS)) {
            throw new JabRefException("Cannot determine current branch (detached HEAD).");
        }
        String shortenRefName = Repository.shortenRefName(fullBranch); // e.g. "main"
        // 1) Try configured upstream (e.g., "refs/remotes/origin/main")
        String trackingBranch = new BranchConfig(repo.getConfig(), shortenRefName).getTrackingBranch();
        ObjectId remoteId = trackingBranch != null ? repo.resolve(trackingBranch + "^{commit}") : null;

        // 2) Fallback to "origin/<branch>" if upstream is not configured but the remote-tracking ref exists
        if (remoteId == null) {
            Ref ref = repo.findRef(Constants.R_REMOTES + "origin/" + shortenRefName);
            if (ref != null) {
                remoteId = ref.getObjectId();
            }
        }
        if (remoteId == null) {
            throw new JabRefException(
                    "Remote tracking branch is missing for '" + shortenRefName + "'. " +
                            "Please set upstream, e.g.: git branch --set-upstream-to=origin/" + shortenRefName + " " + shortenRefName);
        }

        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit local = walk.parseCommit(headId);
            RevCommit remote = walk.parseCommit(remoteId);
            RevCommit base = findMergeBase(repo, local, remote);

            assert base != null : "Could not determine merge base between local and remote.";

            return new RevisionTriple(Optional.ofNullable(base), local, remote);
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

    public static boolean isAncestor(Repository repo, ObjectId maybeAncestor, ObjectId commit) throws IOException {
        try (RevWalk revWalk = new RevWalk(repo)) {
            RevCommit a = revWalk.parseCommit(maybeAncestor);
            RevCommit b = revWalk.parseCommit(commit);
            return revWalk.isMergedInto(a, b);
        }
    }
}
