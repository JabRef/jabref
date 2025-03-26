package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.service.NotificationService;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class GitClientHandler extends GitHandler {
   private final static String GENERAL_ERROR_MESSAGE = Localization.lang("This Git operation failed") + "\n\n" +
        Localization.lang("MOST LIKELY CAUSE: Missing Git credentials.") + "\n" +
        Localization.lang("Please set your credentials by either:") + "\n" +
        "1. " + Localization.lang("Setting GIT_EMAIL and GIT_PW environment variables") + ", " + Localization.lang("or") + "\n" +
        "2. " + Localization.lang("Configuring them in JabRef Preferences") + "\n\n" +
        Localization.lang("Other possible causes:") + "\n" +
        "- " + Localization.lang("Network connectivity issues") + "\n" +
        "- " + Localization.lang("Remote repository rejecting the operation");
    private final NotificationService notificationService;
    private final CliPreferences preferences;

    public GitClientHandler(Path repositoryPath,
                            NotificationService notificationService,
                            CliPreferences preferences) {
        super(repositoryPath, false);
        this.notificationService = notificationService;
        this.preferences = preferences;

        if (preferences != null && preferences.getGitPreferences() != null) {
            this.credentialsProvider = new UsernamePasswordCredentialsProvider(
                    preferences.getGitPreferences().getGitHubUsername(),
                    preferences.getGitPreferences().getGitHubPasskey()
            );
        }
    }

    /**
     * Contains logic for commiting and pushing after a database is saved locally,
     * if the relevant preferences are present.<p>
     * A git commit is created and a 'git pull --rebase' is executed. In the case of
     * an error, the repository is reverted to the commit and a regular pull is executed.
     */
    public void postSaveDatabaseAction() {
        if (isGitRepository() &&
                preferences.getGitPreferences().getAutoPushEnabled()) {
            try {
                createCommitOnCurrentBranch("Automatic update via JabRef", false);
            } catch (GitAPIException | IOException e) {
                return;
            }

            try {
                this.pullAndRebaseOnCurrentBranch();
            } catch (IOException e) {
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
                    this.pull();
                } catch (IOException | GitAPIException ex) {
                    LOGGER.error("Failed to pull");
                    notificationService.notify(Localization.lang("Failed to update repository"));
                    return;
                }
            }

            try {
                this.pushCommitsToRemoteRepository();
            } catch (IOException e) {
                LOGGER.error("Failed to push");
            }
        }
    }

    private void pull() throws IOException, GitAPIException {
        Git git = Git.open(this.repositoryPathAsFile);
        git.pull()
           .setCredentialsProvider(this.credentialsProvider)
           .call();
    }

    private RevCommit getLatestCommit() {
        try {
            Repository repository = new FileRepositoryBuilder()
                    .findGitDir(new File(this.repositoryPath.toString() + "/.git"))
                    .setMustExist(true)
                    .build();
            RevWalk revWalk = new RevWalk(repository);

            ObjectId head = repository.resolve("HEAD");
            return revWalk.parseCommit(head);
        } catch (IOException e) {
            LOGGER.error("Failed to get latest commit");
        }
        return null;
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

    public void showGeneralErrorDialog() {
        notificationService.showErrorDialog(GENERAL_ERROR_MESSAGE);
    }

    public void checkGitRepoAndPullAndDisplayMsg() throws IOException {

        if (!isGitRepository()) {
            handleNonGitRepoOperation();
        }
        if (pullOnCurrentBranch()) {
            notificationService.notify(Localization.lang("Successfully pulled from remote repository"));
        } else {
           showGeneralErrorDialog();
        }
    }

    public void checkGitRepoThenCommitAndPushAndDisplayMsg()throws IOException, GitAPIException {
        if (!isGitRepository()) {
            handleNonGitRepoOperation();
            return;
        }
            boolean commitCreated = this.createCommitOnCurrentBranch(Localization.lang("Automatic update via JabRef"), false);
            if (!commitCreated) {
               showGeneralErrorDialog();
                return;
            }
            boolean successPush = pushCommitsToRemoteRepository();
            if (successPush) {
                notificationService.notify(Localization.lang("Successfully Pushed changes to remote repository"));
            } else {
               showGeneralErrorDialog();
            }
        }

    public void handleNonGitRepoOperation() {
        LOGGER.info("Not a git repository at path: {}", repositoryPath);
        notificationService.notify(Localization.lang("This is not a Git repository"));
    }
}
