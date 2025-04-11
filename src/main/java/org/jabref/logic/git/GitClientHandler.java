package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class GitClientHandler extends GitHandler {
    private final DialogService dialogService;
    private final GitPreferences gitPreferences;

    public GitClientHandler(Path repositoryPath,
                            DialogService dialogService,
                            GitPreferences gitPreferences) {
        super(repositoryPath, false);
        this.dialogService = dialogService;
        this.gitPreferences = gitPreferences;

        this.credentialsProvider = new UsernamePasswordCredentialsProvider(
                gitPreferences.getGitHubUsername(),
                gitPreferences.getGitHubPasskey()
        );
    }

    /**
     * Contains logic for commiting and pushing after a database is saved locally,
     * if the relevant preferences are present.<p>
     * A git commit is created and a 'git pull --rebase' is executed. In the case of
     * an error, the repository is reverted to the commit and a regular pull is executed.
     */
    public void postSaveDatabaseAction() {
        if (this.isGitRepository() && gitPreferences.getAutoPushEnabled()) {
            try {
                this.createCommitOnCurrentBranch("Automatic update via JabRef", false);
            } catch (GitAPIException | IOException e) {
                return;
            }

            try {
                this.pullAndRebaseOnCurrentBranch();
            } catch (IOException | GitAPIException e) {
                // In the case that rebase fails, try revert to previous commit
                // and execute regular pull
                Optional<Ref> headRef = Optional.empty();
                try {
                    headRef = this.getHeadRef();
                } catch (IOException | GitAPIException ex) {
                    LOGGER.error("Cannot find HEAD on current branch");
                }
                if (headRef.isEmpty()) {
                    return;
                }

                try {
                    this.revertToCommit(headRef.get());
                } catch (IOException | GitAPIException ex) {
                    LOGGER.error("Failed to revert to commit");
                }

                try {
                    this.pullOnCurrentBranch();
                } catch (IOException ex) {
                    LOGGER.error("Failed to pull");
                    dialogService.notify(Localization.lang("Failed to update repository"));
                    // TODO: Detect if a merge conflict occurs at this point and resolve
                    return;
                }
            }
            dialogService.notify(Localization.lang("Successfully pulled"));

            try {
                this.pushCommitsToRemoteRepository();
                dialogService.notify(Localization.lang("Succesfully pushed"));
            } catch (IOException e) {
                dialogService.notify(Localization.lang("Failed to push"));
            }
        }
    }

    @Override
    public void pullOnCurrentBranch() throws IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            try {
                git.pull()
                   .setCredentialsProvider(credentialsProvider)
                   .call();
                dialogService.notify(Localization.lang("Successfully updated local repository"));
            } catch (GitAPIException e) {
                dialogService.notify(Localization.lang("Failed to pull from remote repository"));
                LOGGER.error("Git pull failed");
            }
        }
    }

    public void pushCommitsToRemoteRepository() throws IOException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            try {
                git.push()
                   .setCredentialsProvider(credentialsProvider)
                   .call();
                dialogService.notify(Localization.lang("Successfully updated remote repository"));
            } catch (GitAPIException e) {
                dialogService.notify(Localization.lang("Failed to push to remote repository"));
                LOGGER.error("Git push failed", e);
            }
        }
    }

    private Optional<Ref> getHeadRef() throws IOException, GitAPIException {
        return this.getRefForBranch(this.getCurrentlyCheckedOutBranch());
    }

    private void revertToCommit(Ref commit) throws IOException, GitAPIException {
        Git git = Git.open(this.repositoryPathAsFile);
        git.reset()
           .setMode(ResetCommand.ResetType.SOFT)
           .setRef(commit.toString())
           .call();
    }

    private void pullAndRebaseOnCurrentBranch() throws IOException, GitAPIException {
        Git git = Git.open(this.repositoryPathAsFile);
        git.pull()
           .setCredentialsProvider(credentialsProvider)
           .setRebase(true)
           .call();
    }
}
