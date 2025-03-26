package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.AutoPushMode;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;

public class GitClientHandler extends GitHandler {
    private final static String GENERAL_ERROR_MESSAGE = Localization.lang("MOST LIKELY CAUSE: Missing Git credentials.") + "\n" +
            Localization.lang("Please set your credentials by either:") + "\n" +
            "1. " + Localization.lang("Setting GIT_EMAIL and GIT_PW environment variables") + ", " + Localization.lang("or") + "\n" +
            "2. " + Localization.lang("Configuring them in JabRef Preferences") + "\n\n" +
            Localization.lang("Other possible causes:") + "\n" +
            "- " + Localization.lang("Network connectivity issues") + "\n" +
            "- " + Localization.lang("Remote repository rejecting the operation");
    private static final String GIT_PUSH = "Git push";
    private static final String GIT_COMMIT = "Git commit";
    private static final String GIT_PULL = "Git pull";
    private final DialogService dialogService;
    private final CliPreferences preferences;

    public GitClientHandler(Path repositoryPath,
                            DialogService dialogService,
                            CliPreferences preferences) {
        super(repositoryPath, false);
        this.dialogService = dialogService;
        this.preferences = preferences;

        this.credentialsProvider = new UsernamePasswordCredentialsProvider(
                preferences.getGitPreferences().getGitHubUsername(),
                preferences.getGitPreferences().getGitHubPasskey()
        );
    }

    /**
     * Contains logic for commiting and pushing after a database is saved locally,
     * if the relevant preferences are present.<p>
     * A git commit is created and a 'git pull --rebase' is executed. In the case of
     * an error, the repository is reverted to the commit and a regular pull is executed.
     */
    public void postSaveDatabaseAction() {
        if (isGitRepository() &&
                preferences.getGitPreferences().getAutoPushMode() == AutoPushMode.ON_SAVE &&
                preferences.getGitPreferences().getAutoPushEnabled()) {
            // Save BibDatabaseContext of bib files in current HEAD
            RevCommit localCommit = getLatestCommit();
            try {
                createCommitOnCurrentBranch("Automatic update via JabRef", false);
            } catch (GitAPIException | IOException e) {
                return;
            }

            try {
                this.pullAndRebaseOnCurrentBranch();
                RevCommit remoteCommit = getLatestCommit();
            } catch (IOException | GitAPIException e) {
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
                } catch (CheckoutConflictException ex) {
                    // TODO: Resolve
                    LOGGER.info("HERE");
                } catch (IOException | GitAPIException ex) {
                    LOGGER.error("Failed to pull");
                    dialogService.notify(Localization.lang("Failed to update repository"));
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

    private String getFileContents(RevCommit commit, String path) throws IOException {
        try {
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(this.repositoryPathAsFile)
                    .build();

            TreeWalk treeWalk = TreeWalk.forPath(repository, path, commit.getTree());
            if (treeWalk != null) {
                ObjectId objectId = treeWalk.getObjectId(0);
                try (ObjectReader reader = repository.newObjectReader()) {
                    return new String(reader.open(objectId).getBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get file contents");
        }
        return null;
    }

    private RevCommit findMergeBase(RevCommit commit1, RevCommit commit2) throws IOException {
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(this.repositoryPathAsFile)
                .build();

        try (RevWalk revWalk = new RevWalk(repository)) {
            revWalk.markStart(commit1);
            revWalk.markStart(commit2);

            for (RevCommit commit : revWalk) {
                return commit;
            }
        }
        return null;
    }

    private BibDatabaseContext parseBibString(String bibtexContent) throws IOException {
        try (StringReader reader = new StringReader(bibtexContent)) {
            ParserResult result = new BibtexParser(this.preferences.getImportFormatPreferences()).parse(reader);
            return result.getDatabaseContext();
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

    public void showGeneralErrorDialog(String operationType) {
        dialogService.showErrorDialogAndWait(Localization.lang(operationType + "Failed"), GENERAL_ERROR_MESSAGE);
    }

    public void checkGitRepoAndPullAndDisplayMsg() throws IOException {

        if (!isGitRepository()) {
            handleNonGitRepoOperation();
        }
        if (pullOnCurrentBranch()) {
            dialogService.notify(Localization.lang("Successfully pulled from remote repository"));
        } else {
           showGeneralErrorDialog(GIT_PULL);
        }
    }

    public void checkGitRepoThenCommitAndPushAndDisplayMsg()throws IOException, GitAPIException {
        if (!isGitRepository()) {
            handleNonGitRepoOperation();
            return;
        }
            boolean commitCreated = this.createCommitOnCurrentBranch(Localization.lang("Automatic update via JabRef"), false);
            if (!commitCreated) {
               showGeneralErrorDialog(GIT_COMMIT);
                return;
            }
            boolean successPush = pushCommitsToRemoteRepository();
            if (successPush) {
                dialogService.notify(Localization.lang("Successfully Pushed changes to remote repository"));
            } else {
               showGeneralErrorDialog(GIT_PUSH);
            }
        }

    public void handleNonGitRepoOperation() {
        LOGGER.info("Not a git repository at path: {}", repositoryPath);
        dialogService.notify(Localization.lang("This is not a Git repository"));
    }
}
