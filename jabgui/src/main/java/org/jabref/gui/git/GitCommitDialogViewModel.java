package org.jabref.gui.git;

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
import org.jabref.logic.git.GitSyncService;
import org.jabref.logic.git.merge.GitSemanticMergeExecutorImpl;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class GitCommitDialogViewModel extends AbstractViewModel {

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;

    private final StringProperty commitMessage = new SimpleStringProperty("");
    private final BooleanProperty amend = new SimpleBooleanProperty(false);

    private final Validator commitMessageValidator;

    public GitCommitDialogViewModel(
            StateManager stateManager,
            DialogService dialogService,
            GuiPreferences preferences,
            TaskExecutor taskExecutor) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;

        this.commitMessageValidator = new FunctionBasedValidator<>(
                commitMessage,
                message -> message != null && !message.trim().isEmpty(),
                ValidationMessage.error(Localization.lang("Commit message cannot be empty"))
        );
    }

    public void commit(Runnable onSuccess) {
        commitTask()
                .onSuccess(ignore -> {
                    dialogService.notify(Localization.lang("Committed successfully."));
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

            GitHandlerRegistry registry = new GitHandlerRegistry();
            GitSyncService gitSyncService = new GitSyncService(
                    preferences.getImportFormatPreferences(),
                    registry,
                    null,
                    new GitSemanticMergeExecutorImpl(preferences.getImportFormatPreferences())
            );

            boolean committed = gitSyncService.commitLocalChanges(
                    bibFilePath,
                    commitMessage.get().trim(),
                    amend.get()
            );

            if (!committed) {
                throw new JabRefException(Localization.lang("Nothing to commit."));
            }

            return null;
        });
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
