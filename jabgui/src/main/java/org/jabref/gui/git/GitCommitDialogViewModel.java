package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.diff.GitDiffChecker;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;

public class GitCommitDialogViewModel extends AbstractViewModel {

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final GitHandlerRegistry gitHandlerRegistry;
    private final ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileUpdateMonitor;

    private final StringProperty commitMessage = new SimpleStringProperty("");
    private final BooleanProperty amend = new SimpleBooleanProperty(false);

    private final Validator commitMessageValidator;

    public GitCommitDialogViewModel(
            StateManager stateManager,
            DialogService dialogService,
            TaskExecutor taskExecutor,
            GitHandlerRegistry gitHandlerRegistry,
            ImportFormatPreferences importFormatPreferences,
            FileUpdateMonitor fileUpdateMonitor) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.gitHandlerRegistry = gitHandlerRegistry;
        this.importFormatPreferences = importFormatPreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;

        this.commitMessageValidator = new FunctionBasedValidator<>(
                commitMessage,
                message -> message != null && !message.isBlank(),
                ValidationMessage.error(Localization.lang("Commit message cannot be empty"))
        );
    }

    public void commit(Runnable onSuccess) {
        commitTask()
                .onSuccess(_ -> {
                    dialogService.notify(Localization.lang("Committed successfully"));
                    onSuccess.run();
                })
                .onFailure(ex ->
                        dialogService.showErrorDialogAndWait(
                                Localization.lang("Git Commit Failed"),
                                ex.getMessage(),
                                ex
                        )
                )
                .executeWith(taskExecutor);
    }

    public BackgroundTask<Void> commitTask() {
        return BackgroundTask.wrap(() -> {
            doCommit();
            return null;
        });
    }

    public BackgroundTask<DiffDatabases> diffTask() {
        return BackgroundTask.wrap(this::computeDiffDatabases);
    }

    private DiffDatabases computeDiffDatabases() throws JabRefException, IOException {
        TrackedFile trackedFile = getTrackedBibFile();
        Path repositoryRoot = trackedFile.gitHandler().getRepositoryPathAsFile().toPath();
        Path relativeFilePath = repositoryRoot.relativize(trackedFile.bibFilePath());

        BibDatabaseContext headDatabase = GitDiffChecker.checkDiffAgainstLastCommit(
                trackedFile.gitHandler(), relativeFilePath, importFormatPreferences, fileUpdateMonitor);
        BibDatabaseContext workingTreeDatabase = GitDiffChecker.checkSavedWorkingTreeVersion(
                trackedFile.bibFilePath(), importFormatPreferences, fileUpdateMonitor);
        return new DiffDatabases(headDatabase, workingTreeDatabase);
    }

    public record DiffDatabases(BibDatabaseContext headDatabase, BibDatabaseContext workingTreeDatabase) {
    }

    private void doCommit() throws JabRefException, GitAPIException, IOException {
        TrackedFile trackedFile = getTrackedBibFile();
        GitHandler gitHandler = trackedFile.gitHandler();

        GitStatusSnapshot status = GitStatusChecker.checkStatus(gitHandler);
        if (!status.tracking()) {
            throw new JabRefException(Localization.lang("Commit aborted: The file is not under Git version control."));
        }
        if (status.conflict()) {
            throw new JabRefException(Localization.lang("Commit aborted: Local repository has unresolved merge conflicts."));
        }

        String message = commitMessage.get();
        if (message == null || message.isBlank()) {
            DiffDatabases diff = computeDiffDatabases();
            message = generateCommitMessage(diff);
        }

        boolean committed = gitHandler.createCommitOnCurrentBranch(message, amend.get());

        if (!committed) {
            throw new JabRefException(Localization.lang("Nothing to commit."));
        }
    }

    private String generateCommitMessage(DiffDatabases diff) {
        BibDatabaseDiff databaseDiff = BibDatabaseDiff.compare(
                diff.headDatabase(),
                diff.workingTreeDatabase()
        );

        int added = 0;
        int deleted = 0;
        int modified = 0;

        for (BibEntryDiff entryDiff : databaseDiff.getEntryDifferences()) {
            if (entryDiff.originalEntry() == null) {
                added++;
            } else if (entryDiff.newEntry() == null) {
                deleted++;
            } else {
                modified++;
            }
        }
        List<String> changes = new ArrayList<>();

        if (added > 0) {
            changes.add("Add " + added + (added == 1 ? " entry" : " entries"));
        }

        if (deleted > 0) {
            changes.add("Delete " + deleted + (deleted == 1 ? " entry" : " entries"));
        }

        if (modified > 0) {
            changes.add("Modify " + modified + (modified == 1 ? " entry" : " entries"));
        }

        return String.join(", ", changes);

    }
    private TrackedFile getTrackedBibFile() throws JabRefException {
        Optional<BibDatabaseContext> activeDatabaseOpt = stateManager.getActiveDatabase();
        if (activeDatabaseOpt.isEmpty()) {
            throw new JabRefException(Localization.lang("No library open"));
        }

        Optional<Path> bibFilePathOpt = activeDatabaseOpt.get().getDatabasePath();
        if (bibFilePathOpt.isEmpty()) {
            throw new JabRefException(Localization.lang("No library file path. Please save the library to a file first."));
        }

        Path bibFilePath = bibFilePathOpt.get();
        Optional<Path> repoRootOpt = GitHandler.findRepositoryRoot(bibFilePath);
        if (repoRootOpt.isEmpty()) {
            throw new JabRefException(Localization.lang("Commit aborted: Path is not inside a Git repository."));
        }

        GitHandler gitHandler = gitHandlerRegistry.get(repoRootOpt.get());
        return new TrackedFile(gitHandler, bibFilePath);
    }

    private record TrackedFile(GitHandler gitHandler, Path bibFilePath) {
    }

    public StringProperty commitMessageProperty() {
        return commitMessage;
    }

    public BooleanProperty amendProperty() {
        return amend;
    }

    public ValidationStatus commitMessageValidation() {
        return commitMessageValidator.getValidationStatus();
    }
}
