package org.jabref.logic.git;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitActionExecutor {

    private final static Logger LOGGER = LoggerFactory.getLogger(GitActionExecutor.class);

    private final Git git;
    private final Path repository;

    public GitActionExecutor(Git git) {
        this.git = git;
        File gitRepository = git.getRepository().getDirectory(); // this the file path including .git at the end
        this.repository = gitRepository.getParentFile().toPath();
    }

    public void add(Path path) throws GitException {
        try {
            Path relativePath = repository.relativize(path);
            git.add().addFilepattern(relativePath.toString()).call();
            LOGGER.debug("File added to staging: " + path);
        } catch (GitAPIException e) {
            throw new GitException("Failed to add file " + path + " to staging area", e);
        }
    }

    public void add(List<Path> paths) throws GitException {
            for (Path path : paths) {
                add(path);
            }
    }

    public void commit(String message, boolean append) throws GitException {
        try {
            git.commit().setMessage(message).setAmend(append).call();
            LOGGER.debug("Commit successful with message: " + message);
        } catch (GitAPIException e) {
            throw new GitException("Commit failed", e);
        }
    }

    public void push(String remote, String branch) throws GitException {
        try {
            git.push().setRemote(remote).add(branch).call();
            LOGGER.debug("Pushed to remote: " + remote + ", branch: " + branch);
        } catch (GitAPIException e) {
            throw new GitException("Push failed", e);
        }
    }

    // TODO: test
    public void push() throws GitException {
        try {
            git.push().call();
            LOGGER.debug("Pushed to default remote and branch.");
        } catch (GitAPIException e) {
            throw new GitException("Push failed", e);
        }
    }

    public void pull(boolean withRebase, String remote, String branch) throws GitException {
        try {
            git.pull()
               .setRebase(withRebase)
               .setRemote(remote)
               .setRemoteBranchName(branch)
               .call();
            LOGGER.debug("Pulled from remote: " + remote + ", branch: " + branch);
        } catch (GitAPIException e) {
            throw new GitException("Pull failed", e);
        }
    }

    public void pull(String remote, String branch) throws GitException {
        pull(false, remote, branch);
    }

    public Git getGit() {
        return git;
    }

    public void undoPull() throws GitException {
        try {
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
            LOGGER.debug("Last pull undone (hard reset).");
        } catch (GitAPIException e) {
            throw new GitException("Undo pull failed", e);
        }
    }

    // TODO: test whether all changes are stashed or only the stages ones
    public void stash() throws GitException {
        try {
            git.stashCreate().call();
            LOGGER.debug("Current changes stashed.");
        } catch (GitAPIException e) {
            throw new GitException("Stash failed");
        }
    }

    // TODO: test that it applies the latest stash in case there are multiple
    public void applyLatestStash() throws GitException {
        try {
            git.stashApply().call();
            LOGGER.debug("Stash applied.");
        } catch (GitAPIException e) {
            // TODO: in this case the user must be informed
            throw new GitException("Unstash failed", e);
        }
    }
}
