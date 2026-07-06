package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.validation.ValidationConstraints;
import org.jabref.gui.validation.ValidationMessage;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.jfxcore.validation.property.ConstrainedStringProperty;
import org.jfxcore.validation.property.SimpleConstrainedStringProperty;

public class GitCommitDialogViewModel extends AbstractViewModel {

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final GitHandlerRegistry gitHandlerRegistry;

    private final ConstrainedStringProperty<ValidationMessage> commitMessage;
    private final BooleanProperty amend = new SimpleBooleanProperty(false);

    public GitCommitDialogViewModel(
            StateManager stateManager,
            DialogService dialogService,
            TaskExecutor taskExecutor,
            GitHandlerRegistry gitHandlerRegistry) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.gitHandlerRegistry = gitHandlerRegistry;

        this.commitMessage = new SimpleConstrainedStringProperty<>(
                "",
                ValidationConstraints.predicate(
                        message -> !StringUtil.isBlank(message),
                        ValidationMessage.error(Localization.lang("Commit message cannot be empty"))));
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

    private void doCommit() throws JabRefException, GitAPIException, IOException {
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

        String message = commitMessage.get();
        if (message == null || message.isBlank()) {
            message = Localization.lang("Update references");
        }

        boolean committed = gitHandler.createCommitOnCurrentBranch(message, amend.get());
        // TODO: Replace control-flow-by-exception with a proper control structure
        if (!committed) {
            throw new JabRefException(Localization.lang("Nothing to commit."));
        }
    }

    public ConstrainedStringProperty<ValidationMessage> commitMessageProperty() {
        return commitMessage;
    }

    public BooleanProperty amendProperty() {
        return amend;
    }
}
