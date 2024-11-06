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

public class GitStatus {

    private Git git;

    public GitStatus(Git git){
        this.git = git;
    }

    // i am assuming the first method in the UML is supposed to be called
    // getBranchName*s*() with 's' plural? Just fetching all branches (remote
    // as well as local), equivalent to git branch -a
    // TODO: @amer check this out pls
    public List<String> getBranchNames() throws GitAPIException {
        List<Ref> localBranches = this.git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        List<String> branchNames = new ArrayList<>();
        for (Ref branch : localBranches) {
            branchNames.add(branch.getName());
        }
        return branchNames;
    }

    // renaming these two methods to Untracked
    public boolean hasUntrackedFiles() throws GitAPIException {
        Status status = this.git.status().call();
        return status.getUntracked().isEmpty();
    }

    public Set<String> getUntrackedFiles() throws GitAPIException {
        Status status = this.git.status().call();
        return status.getUntracked();
    }

    // tracked files just means those files that are staged

    public boolean hasTrackedFiles() throws GitAPIException {
        Status status = this.git.status().call();
        return status.getAdded().isEmpty();
    }

    public Set<String> getTrackedFiles() throws GitAPIException {
        Status status = this.git.status().call();
        return status.getAdded();
    }

    public boolean hasUnpushedCommits() throws IOException {
        boolean hasUnpushedCommits = false;

        Repository repo = git.getRepository(); // we need the name of the branch we are on, on which we even check if commits are unpushed
        String currentBranchName = repo.getBranch();

        Ref currentLocalBranchRef = repo.exactRef(currentBranchName);
        Ref remoteBranchRef = repo.findRef("refs/remotes/origin/" + currentBranchName);

        //now that we have the references, we can comapre histories
        try  (RevWalk walk = new RevWalk(git.getRepository())) {

            ObjectId localBranchHead = currentLocalBranchRef.getObjectId(); // need obj ids to feed into walk start
            ObjectId remoteBranchHead = remoteBranchRef.getObjectId();

            // Start walking from the local branch head
            walk.markStart(walk.parseCommit(localBranchHead));

            //walk.markUninteresting();
            walk.markUninteresting(walk.parseCommit(remoteBranchHead)); // anything on the remote we skip


            //invariant: if walk == empty, then upto date, else we have unpushed commits
            for (RevCommit commit : walk) {
                System.out.println("Unpushed commit: " + commit.getFullMessage());
                hasUnpushedCommits = true;
            }

            if (!hasUnpushedCommits) {
                System.out.println("All commits are pushed.");
            }
        }

        return hasUnpushedCommits;


    }

}
