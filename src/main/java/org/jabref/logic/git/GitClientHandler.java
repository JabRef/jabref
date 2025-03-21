package org.jabref.logic.git;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.AutoPushMode;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class GitClientHandler extends GitHandler {
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
        if (this.isGitRepository() &&
                preferences.getGitPreferences().getAutoPushMode() == AutoPushMode.ON_SAVE &&
                preferences.getGitPreferences().getAutoPushEnabled()) {
            // Save BibDatabaseContext of bib files in current HEAD
            List<Optional<BibDatabaseContext>> baseBibContextList;
            try {
                baseBibContextList = this.getChangedBibDatabaseContextList(this.getChangedFilesFromHeadToIndex());
            } catch (IOException | GitAPIException e) {
                LOGGER.error("Failed to save changes from HEAD to index");
            }

            try {
                this.createCommitOnCurrentBranch("Automatic update via JabRef", false);
            } catch (GitAPIException | IOException e) {
                return;
            }

            try {
                this.pullAndRebaseOnCurrentBranch();
                List<Optional<BibDatabaseContext>> remoteBibContextList;
                try {
                    remoteBibContextList = this.getChangedBibDatabaseContextList(this.getChangedFilesFromHeadToIndex());
                } catch (IOException | GitAPIException e) {
                    LOGGER.error("Failed to save changes from HEAD to index");
                }
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
                    // TODO: Detect if a merge conflict occurs at this point and resolve using baseBibContextList and remoteBibContextList
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

    @Override
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

    /**
     * Save BibDatabaseContext for all bib files from HEAD to index
     *
     * @param pathList List of Paths to retrieve BibDatabaseContext objects from
     * @return List of Optional BibDatabaseContext objects from
     */
    private List<Optional<BibDatabaseContext>> getChangedBibDatabaseContextList(List<Path> pathList) throws IOException, GitAPIException {
        this.stageAllChangesToCurrentBranch();
        return pathList
                .stream()
                .map(this::loadBibDatabaseContext)
                .toList();
    }

    private List<Path> getChangedFilesFromHeadToIndex() throws IOException, GitAPIException {
        Git git = Git.open(this.repositoryPathAsFile);
        return git.status()
                  .call()
                  .getChanged()
                  .stream()
                  .map(Path::of)
                  .toList();
    }

    private Optional<BibDatabaseContext> loadBibDatabaseContext(Path bibFilePath) {
        if (!FileUtil.isBibFile(bibFilePath)) {
            return Optional.empty();
        }

        ImportFormatPreferences importFormatPreferences = preferences.getImportFormatPreferences();

        try (BufferedReader reader = Files.newBufferedReader(bibFilePath, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(importFormatPreferences).parse(reader);

            return Optional.of(result.getDatabaseContext());
        } catch (IOException e) {
            LOGGER.error("Failed to load database context");
            return Optional.empty();
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
        dialogService.notify(Localization.lang("Successfully updated local repository"));
    }
}
