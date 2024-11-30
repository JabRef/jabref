package org.jabref.logic.git;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GitStatus {

    private final static Logger LOGGER = LoggerFactory.getLogger(GitStatus.class);

    private final Git git;

    GitStatus(Git git) {
        this.git = git;
    }

    // TODO: decide whether this is needed
    List<String> getBranchNames() throws GitException {
        List<Ref> localBranches;
        try {
            localBranches = this.git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        } catch (GitAPIException e) {
            throw new GitException("An error occurred while fetching branch names", e);
        }
        List<String> branchNames = new ArrayList<>();
        for (Ref branch : localBranches) {
            branchNames.add(branch.getName());
        }
        return branchNames;
    }

    boolean hasUntrackedFiles() throws GitException {
        return !getUntrackedFiles().isEmpty();
    }

    Set<String> getUntrackedFiles() throws GitException {
        Status status;
        try {
            status = this.git.status().call();
        } catch (GitAPIException e) {
            throw new GitException("Failed to get git status", e);
        }
        return status.getUntracked();
    }

    boolean hasTrackedFiles() throws GitException {
        return !getTrackedFiles().isEmpty();
    }

    Set<String> getTrackedFiles() throws GitException {
        Status status;
        try {
            status = this.git.status().call();
        } catch (GitAPIException e) {
            throw new GitException("Failed to get git status", e);
        }
        return status.getAdded();
    }

    // TODO: wouldn't it be sufficient to check whether the latest commit on the local branch
    //  is present on the remote or not?
    boolean hasUnpushedCommits() throws IOException {
        boolean hasUnpushedCommits = false;

        Repository repo = git.getRepository(); // we need the name of the branch we are on, on which we even check if commits are unpushed
        String currentBranchName = repo.getBranch();

        Ref currentLocalBranchRef = repo.exactRef(currentBranchName);
        Ref remoteBranchRef = repo.findRef("refs/remotes/origin/" + currentBranchName);

        // now that we have the references, we can comapre histories
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            ObjectId localBranchHead = currentLocalBranchRef.getObjectId(); // need obj ids to feed into walk start
            ObjectId remoteBranchHead = remoteBranchRef.getObjectId();

            // Start walking from the local branch head
            walk.markStart(walk.parseCommit(localBranchHead));

            // walk.markUninteresting();
            walk.markUninteresting(walk.parseCommit(remoteBranchHead)); // anything on the remote we skip

            // invariant: if walk == empty, then upto date, else we have unpushed commits
            for (RevCommit commit : walk) {
                LOGGER.debug("Unpushed commit: {}", commit.getFullMessage());
                hasUnpushedCommits = true;
            }

            if (!hasUnpushedCommits) {
                LOGGER.debug("All commits are pushed.");
            }
        }

        return hasUnpushedCommits;
    }
}
