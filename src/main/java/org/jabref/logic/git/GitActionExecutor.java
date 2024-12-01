package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.l10n.Localization;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RebaseCommand;
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
    private final GitAuthenticator gitAuthenticator;

    private ObjectId previousHead;

    GitActionExecutor(Git git, GitAuthenticator gitAuthenticator) {
        this.git = git;
        File gitRepository = git.getRepository().getDirectory(); // this the file path including .git at the end
        this.repository = gitRepository.getParentFile().toPath();
        this.gitAuthenticator = gitAuthenticator;
    }

    // TODO: test case for if path is not inside repo, test the first branch
    void add(Path path) throws GitException {
        if (!path.startsWith(repository)) {
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
            gitAuthenticator.authenticate(pushCommand);
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

    /**
     * pulls from the remote repository after storing the current HEAD in case the pull operation results in conflicts
     * and the pull operation needs to be undone.
     *
     * @param withRebase whether to rebase the pull operation. if true and a conflict occurs, a rebase --abort is
     *                   performed to exit the rebase state which results in the HEAD being reset to the previous state.
     *                   if false and a conflict occurs, the pull operation needs to be undone afterward.
     * @param remote the remote repository to pull from. If null, the default remote is used.
     * @param branch the branch to pull from. If null, the current branch is used.
     * @throws GitConflictException if the pull operation results in conflicts
     * @throws GitException if the pull operation fails for any other reason
     */
    void pull(boolean withRebase, String remote, String branch) throws GitException {
        try {

            previousHead = git.getRepository().resolve(Constants.HEAD);

            PullCommand pullCommand = git.pull()
                                         .setRebase(withRebase)
                                         .setRemote(remote);
            gitAuthenticator.authenticate(pullCommand);
            if (branch != null) {
                pullCommand.setRemoteBranchName(branch);
            }
            pullCommand.call();
            LOGGER.debug("Pulled from remote: {}, branch: {}", remote, branch);
            if (git.status().call().getConflicting().isEmpty()) {
                return;
            }
            LOGGER.debug("Git pull resulted in conflicts.");
            if (withRebase) {
                git.rebase().setOperation(RebaseCommand.Operation.ABORT).call();
            }
            throw new GitConflictException("Git pull resulted in conflicts.", Localization.lang("Git pull resulted in conflicts."));
        } catch (GitAPIException | IOException e) {
            throw new GitException("Failed to perform git pull operation", e);
        }
    }

    // TODO: test this
    /**
     * calls {@link #pull(boolean, String, String)} with default remote and current branch
     */
    void pull(boolean withRebase) throws GitException {
        pull(withRebase, null, null);
    }

    /**
     * Undoes the latest pull operation by hard resetting the HEAD to the previous state. It only works if
     * the pull operation was performed using the same instance as the one calling this method.
     *
     * @throws GitException if the undo operation fails for any reason
     */
    void undoPull() throws GitException {
        try {
            if (previousHead == null) {
                throw new GitException("Cannot undo pull: previous HEAD not recorded.");
            }
            git.reset()
               .setRef(previousHead.getName())
               .setMode(ResetCommand.ResetType.HARD)
               .call();
            LOGGER.debug("Last pull undone (hard reset to previous HEAD).");
        } catch (GitAPIException e) {
            throw new GitException("Failed to undo latest git pull", e);
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

    Git getGit() {
        return git;
    }
}
