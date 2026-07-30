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
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.GitSyncService;
import org.jabref.logic.git.model.PushResult;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.eclipse.jgit.api.errors.GitAPIException;

public class GitCommitDialogViewModel extends AbstractViewModel {

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final GitHandlerRegistry gitHandlerRegistry;
    private final GuiPreferences guiPreferences;

    private final StringProperty commitMessage = new SimpleStringProperty("");
    private final BooleanProperty amend = new SimpleBooleanProperty(false);

    private final Validator commitMessageValidator;

    public GitCommitDialogViewModel(
            StateManager stateManager,
            DialogService dialogService,
            TaskExecutor taskExecutor,
            GitHandlerRegistry gitHandlerRegistry,
            GuiPreferences guiPreferences) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.gitHandlerRegistry = gitHandlerRegistry;
        this.guiPreferences = guiPreferences;

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
                .onFailure(ex ->
                        dialogService.showErrorDialogAndWait(
                                ex instanceof PushFailedException
                                ? Localization.lang("Git Push Failed")
                                : Localization.lang("Git Commit Failed"),
                                ex.getMessage(),
                                ex
                        )
                )
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
            case COMMITTED_WITHOUT_PUSH ->
                    Localization.lang("Nothing to push. Local branch is up to date.");
        };
    }

    private CommitOutcome doCommitAndPush() throws JabRefException, GitAPIException, IOException {
        ResolvedRepository repository = resolveRepository();
        commitOn(repository);
        return pushTo(repository);
    }

    private void doCommit() throws JabRefException, GitAPIException, IOException {
        commitOn(resolveRepository());
    }

    private ResolvedRepository resolveRepository() throws JabRefException {
        Optional<BibDatabaseContext> activeDatabaseOpt = stateManager.getActiveDatabase();
        if (activeDatabaseOpt.isEmpty()) {
            throw new JabRefException(Localization.lang("No library open"));
        }

        BibDatabaseContext dbContext = activeDatabaseOpt.get();
        Optional<Path> bibFilePathOpt = dbContext.getDatabasePath();
        if (bibFilePathOpt.isEmpty()) {
            throw new JabRefException(Localization.lang("No library file path. Please save the library to a file first."));
        }

        Path bibFilePath = bibFilePathOpt.get();
        Optional<Path> repoRootOpt = GitHandler.findRepositoryRoot(bibFilePath);
        if (repoRootOpt.isEmpty()) {
            throw new JabRefException(Localization.lang("Commit aborted: Path is not inside a Git repository."));
        }

        GitHandler gitHandler = gitHandlerRegistry.get(repoRootOpt.get());

        GitStatusSnapshot status = GitStatusChecker.checkStatus(gitHandler);
        if (!status.tracking()) {
            throw new JabRefException(Localization.lang("Commit aborted: The file is not under Git version control."));
        }
        if (status.conflict()) {
            throw new JabRefException(Localization.lang("Commit aborted: Local repository has unresolved merge conflicts."));
        }

        return new ResolvedRepository(dbContext, bibFilePath, gitHandler);
    }

    private void commitOn(ResolvedRepository repository) throws JabRefException, GitAPIException, IOException {
        String message = commitMessage.get();
        if (StringUtil.isBlank(message)) {
            message = Localization.lang("Update references");
        }

        boolean committed = repository.gitHandler().createCommitOnCurrentBranch(message, amend.get());
        // TODO: Replace control-flow-by-exception with a proper control structure
        if (!committed) {
            throw new JabRefException(Localization.lang("Nothing to commit."));
        }
    }

    private CommitOutcome pushTo(ResolvedRepository repository) throws PushFailedException {
        try {
            PushResult result = GitSyncService.create(guiPreferences.getImportFormatPreferences(), gitHandlerRegistry)
                                              .push(repository.database(), repository.bibFilePath());
            return result.noop() ? CommitOutcome.COMMITTED_WITHOUT_PUSH : CommitOutcome.COMMITTED_AND_PUSHED;
        } catch (JabRefException | GitAPIException | IOException e) {
            throw new PushFailedException(e);
        }
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
        COMMITTED_WITHOUT_PUSH
    }

    /// Marks a failure that happened once the commit already existed, so the user is not told the commit itself failed.
    private static class PushFailedException extends JabRefException {
        PushFailedException(Throwable cause) {
            super(cause.getLocalizedMessage(), cause);
        }
    }
}
