package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.GitSyncService;
import org.jabref.logic.git.conflicts.GitConflictResolverStrategy;
import org.jabref.logic.git.merge.GitSemanticMergeExecutor;
import org.jabref.logic.git.merge.GitSemanticMergeExecutorImpl;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.errors.GitAPIException;

public class GitPullAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences guiPreferences;
    private final TaskExecutor taskExecutor;

    public GitPullAction(DialogService dialogService,
                         StateManager stateManager,
                         GuiPreferences guiPreferences,
                         TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.guiPreferences = guiPreferences;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            dialogService.showErrorDialogAndWait("No database open", "Please open a database before pulling.");
            return;
        }

        BibDatabaseContext database = stateManager.getActiveDatabase().get();
        if (database.getDatabasePath().isEmpty()) {
            dialogService.showErrorDialogAndWait("No .bib file path", "Cannot pull from Git: No file is associated with this database.");
            return;
        }

        Path bibFilePath = database.getDatabasePath().get();
        GitHandler handler = new GitHandler(bibFilePath.getParent());
        GitConflictResolverDialog dialog = new GitConflictResolverDialog(dialogService, guiPreferences);
        GitConflictResolverStrategy resolver = new GuiConflictResolverStrategy(dialog);
        GitSemanticMergeExecutor mergeExecutor = new GitSemanticMergeExecutorImpl(guiPreferences.getImportFormatPreferences());

        GitSyncService syncService = new GitSyncService(guiPreferences.getImportFormatPreferences(), handler, resolver, mergeExecutor);
        GitStatusViewModel statusViewModel = new GitStatusViewModel(stateManager, bibFilePath);
        GitPullViewModel viewModel = new GitPullViewModel(syncService, statusViewModel);

        BackgroundTask
                .wrap(() -> viewModel.pull())
                .onSuccess(result -> {
                    if (result.isSuccessful()) {
                        dialogService.showInformationDialogAndWait("Git Pull", "Successfully merged and updated.");
                    } else {
                        dialogService.showWarningDialogAndWait("Git Pull", "Merge completed with conflicts.");
                    }
                })
                .onFailure(ex -> {
                    if (ex instanceof JabRefException e) {
                        dialogService.showErrorDialogAndWait("Git Pull Failed", e.getLocalizedMessage());
                    } else if (ex instanceof GitAPIException e) {
                        dialogService.showErrorDialogAndWait("Git Pull Failed", "An unexpected Git error occurred: " + e.getLocalizedMessage());
                    } else if (ex instanceof IOException e) {
                        dialogService.showErrorDialogAndWait("Git Pull Failed", "I/O error: " + e.getLocalizedMessage());
                    } else {
                        dialogService.showErrorDialogAndWait("Git Pull Failed", "Unexpected error: " + ex.getLocalizedMessage());
                    }
                })
                .executeWith(taskExecutor);
    }
}
