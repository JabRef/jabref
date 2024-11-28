package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GitActionExecutor {

    private final static Logger LOGGER = LoggerFactory.getLogger(GitActionExecutor.class);

    private final Git git;
    private final Path repository;

    private ObjectId previousHead;

    GitActionExecutor(Git git) {
        this.git = git;
        File gitRepository = git.getRepository().getDirectory(); // this the file path including .git at the end
        this.repository = gitRepository.getParentFile().toPath();
    }

    // TODO: test case for if path is not inside repo, test the first branch
    void add(Path path) throws GitException {
        if(!path.startsWith(repository)){
            throw new GitException("Given path not inside repository.");
        }
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
            PushCommand pushCommand = git.push();
            GitAuthenticator.authenticate(pushCommand);
            if (branch != null) {
                pushCommand.add(branch);
            }
            pushCommand.setRemote(remote)
                       .call();
            LOGGER.debug("Pushed to remote: {}, branch: {}", remote, branch);
        } catch (GitAPIException e) {
            throw new GitException("Push failed", e);
        }
    }


    void push() throws GitException {
        push(null, null);
    }

    void pull(boolean withRebase, String remote, String branch) throws GitException {
        try {

            previousHead = git.getRepository().resolve(Constants.HEAD);

            PullCommand pullCommand = git.pull();
            GitAuthenticator.authenticate(pullCommand);
            pullCommand.setRebase(withRebase)
                       .setRemote(remote)
                       .setRemoteBranchName(branch)
                       .call();
            LOGGER.debug("Pulled from remote: {}, branch: {}", remote, branch);
        } catch (
                GitAPIException |
                IOException e) {
            throw new GitException("Pull failed", e);
        }
    }

    // TODO: test this
    void pull(boolean withRebase) throws GitException {
        pull(withRebase, null, null);
    }

    Git getGit() {
        return git;
    }


    // TODO: test this
    void undoPull() throws GitException {
        try {
            if (previousHead != null) {
                git.reset()
                   .setRef(previousHead.getName())
                   .setMode(ResetCommand.ResetType.HARD)
                   .call();
                LOGGER.debug("Last pull undone (hard reset to previous HEAD).");
            } else {
                throw new GitException("Cannot undo pull: previous HEAD not recorded.");
            }
        } catch (GitAPIException e) {
            throw new GitException("Undo pull failed", e);
        }
    }

    // TODO: test whether all changes are stashed or only the staged ones
    void stash() throws GitException {
        try {
            git.stashCreate().setIncludeUntracked(false).call();
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
