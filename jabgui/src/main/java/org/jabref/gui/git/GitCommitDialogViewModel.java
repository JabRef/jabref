package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
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
import org.jabref.logic.git.GitSyncService;
import org.jabref.logic.git.diff.GitDiffChecker;
import org.jabref.logic.git.model.PushResult;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.util.GitExceptionUtil;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class GitCommitDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitCommitDialogViewModel.class);

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
        commitAction(onSuccess, commitTask());
    }

    public void commitAndPush(Runnable onSuccess) {
        commitAction(onSuccess, commitAndPushTask());
    }

    private void commitAction(Runnable onSuccess, BackgroundTask<CommitOutcome> task) {
        task
                .onSuccess(outcome -> {
                    dialogService.notify(messageFor(outcome));
                    onSuccess.run();
                })
                .onFailure(ex -> {
                    LOGGER.warn("Git commit failed", ex);
                    String message = localizedFailureMessage(ex);
                    dialogService.showErrorDialogAndWait(
                            ex instanceof PushFailedException
                            ? Localization.lang("Git push failed")
                            : Localization.lang("Git commit failed"),
                            message
                    );
                })
                .executeWith(taskExecutor);
    }

    private BackgroundTask<CommitOutcome> commitTask() {
        return BackgroundTask.wrap(() -> {
            doCommit();
            return CommitOutcome.COMMITTED;
        });
    }

    private BackgroundTask<CommitOutcome> commitAndPushTask() {
        return BackgroundTask.wrap(this::doCommitAndPush);
    }

    private String messageFor(CommitOutcome outcome) {
        return switch (outcome) {
            case COMMITTED ->
                    Localization.lang("Committed successfully");
            case COMMITTED_AND_PUSHED ->
                    Localization.lang("Committed and pushed successfully");
            case PUSHED ->
                    Localization.lang("Pushed successfully.");
            case NOTHING_TO_PUSH ->
                    Localization.lang("Nothing to push. Local branch is up to date.");
        };
    }

    private CommitOutcome doCommitAndPush() throws JabRefException, GitAPIException, IOException {
        ResolvedRepository repository = resolveRepository();
        // Unlike doCommit(), a missing commit here is not fatal: a previous attempt may already have
        // committed and only the push failed, so retrying should still try to push that commit.
        boolean committedNow = commitCurrent(repository);
        return pushTo(repository, committedNow);
    }

    private String commitMessageOrDefault() {
        String message = commitMessage.get();
        return StringUtil.isBlank(message) ? Localization.lang("Update references") : message;
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
        commitOn(resolveRepository());
    }

    private ResolvedRepository resolveRepository() throws JabRefException {
        TrackedFile trackedFile = getTrackedBibFile();
        GitHandler gitHandler = trackedFile.gitHandler();

        GitStatusSnapshot status = GitStatusChecker.checkStatusOrThrow(gitHandler);
        if (!status.tracking()) {
            throw new JabRefException(Localization.lang("Commit aborted: The file is not under Git version control."));
        }
        if (status.conflict()) {
            throw new JabRefException(Localization.lang("Commit aborted: Local repository has unresolved merge conflicts."));
        }

        // getTrackedBibFile() already ensured an active database exists.
        BibDatabaseContext dbContext = stateManager.getActiveDatabase()
                                                   .orElseThrow(() -> new JabRefException(Localization.lang("No library open")));
        return new ResolvedRepository(dbContext, trackedFile.bibFilePath(), gitHandler);
    }

    private void commitOn(ResolvedRepository repository) throws JabRefException, GitAPIException, IOException {
        // TODO: Replace control-flow-by-exception with a proper control structure
        if (!commitCurrent(repository)) {
            throw new JabRefException(Localization.lang("Nothing to commit."));
        }
    }

    private boolean commitCurrent(ResolvedRepository repository) throws GitAPIException, IOException {
        return repository.gitHandler().createCommitOnCurrentBranch(commitMessageOrDefault(), amend.get());
    }

    private String localizedFailureMessage(Throwable throwable) {
        if (throwable instanceof JabRefException jabRefException) {
            return jabRefException.getLocalizedMessage();
        }
        if (GitExceptionUtil.isLockFailure(throwable)) {
            return Localization.lang("The Git repository is locked. Close other Git, JabRef, or IDE processes and try again.");
        }
        return Localization.lang("Could not commit the changes. Please check the repository and try again.");
    }

    private CommitOutcome pushTo(ResolvedRepository repository, boolean committedNow) throws PushFailedException {
        try {
            PushResult result = GitSyncService.create(importFormatPreferences, gitHandlerRegistry)
                                              .push(repository.database(), repository.bibFilePath());
            if (result.noop() && !committedNow) {
                return CommitOutcome.NOTHING_TO_PUSH;
            }
            return committedNow ? CommitOutcome.COMMITTED_AND_PUSHED : CommitOutcome.PUSHED;
        } catch (JabRefException | GitAPIException | IOException e) {
            throw new PushFailedException(e, committedNow);
        }
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

        Path bibFilePath = GitHandler.resolveToRealPath(bibFilePathOpt.get());
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

    private record ResolvedRepository(BibDatabaseContext database, Path bibFilePath, GitHandler gitHandler) {
    }

    /// What a finished dialog action actually achieved, so the user can be told the truth about it.
    private enum CommitOutcome {
        COMMITTED,
        COMMITTED_AND_PUSHED,
        PUSHED,
        NOTHING_TO_PUSH
    }

    private static class PushFailedException extends JabRefException {
        /// Claiming "the commit was saved" is only honest when this attempt actually created one:
        /// the push may also fail with nothing newly committed (e.g. retrying after a failed push).
        PushFailedException(Throwable cause, boolean committedNow) {
            super(committedNow
                  ? Localization.lang("The commit was saved locally, but the push failed: %0", causeMessage(cause))
                  : Localization.lang("Push failed: %0", causeMessage(cause)), cause);
        }

        private static String causeMessage(Throwable cause) {
            String message = cause.getLocalizedMessage();
            return message != null ? message : Localization.lang("Git push failed");
        }
    }
}
