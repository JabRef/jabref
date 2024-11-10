package org.jabref.logic.git;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GitActionExecutor {

    private final static Logger LOGGER = LoggerFactory.getLogger(GitActionExecutor.class);

    private final Git git;
    private final Path repository;

    GitActionExecutor(Git git) {
        this.git = git;
        File gitRepository = git.getRepository().getDirectory(); // this the file path including .git at the end
        this.repository = gitRepository.getParentFile().toPath();
    }

    void add(Path path) throws GitException {
        try {
            Path relativePath = repository.relativize(path);
            git.add().addFilepattern(relativePath.toString()).call();
            LOGGER.debug("File added to staging: {}", path);
        } catch (GitAPIException e) {
            throw new GitException("Failed to add file " + path + " to staging area", e);
        }
    }

    void add(List<Path> paths) throws GitException {
            for (Path path : paths) {
                add(path);
            }
    }

    void commit(String message, boolean append) throws GitException {
        try {
            git.commit().setMessage(message).setAmend(append).call();
            LOGGER.debug("Commit successful with message: {}", message);
        } catch (GitAPIException e) {
            throw new GitException("Commit failed", e);
        }
    }

    void push(String remote, String branch) throws GitException {
        try {
            git.push().setRemote(remote).add(branch).call();
            LOGGER.debug("Pushed to remote: {}, branch: {}", remote, branch);
        } catch (GitAPIException e) {
            throw new GitException("Push failed", e);
        }
    }

    // TODO: test
    void push() throws GitException {
        try {
            git.push().call();
            LOGGER.debug("Pushed to default remote and branch.");
        } catch (GitAPIException e) {
            throw new GitException("Push failed", e);
        }
    }

    void pull(boolean withRebase, String remote, String branch) throws GitException {
        try {
            git.pull()
               .setRebase(withRebase)
               .setRemote(remote)
               .setRemoteBranchName(branch)
               .call();
            LOGGER.debug("Pulled from remote: {}, branch: {}", remote, branch);
        } catch (GitAPIException e) {
            throw new GitException("Pull failed", e);
        }
    }

    void pull(String remote, String branch) throws GitException {
        pull(false, remote, branch);
    }

    Git getGit() {
        return git;
    }

    void undoPull() throws GitException {
        try {
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
            LOGGER.debug("Last pull undone (hard reset).");
        } catch (GitAPIException e) {
            throw new GitException("Undo pull failed", e);
        }
    }

    // TODO: test whether all changes are stashed or only the stages ones
    void stash() throws GitException {
        try {
            git.stashCreate().call();
            LOGGER.debug("Current changes stashed.");
        } catch (GitAPIException e) {
            throw new GitException("Stash failed");
        }
    }

    // TODO: test that it applies the latest stash in case there are multiple
    void applyLatestStash() throws GitException {
        try {
            git.stashApply().call();
            LOGGER.debug("Stash applied.");
        } catch (GitAPIException e) {
            // TODO: in this case the user must be informed
            throw new GitException("Unstash failed", e);
        }
    }
}
