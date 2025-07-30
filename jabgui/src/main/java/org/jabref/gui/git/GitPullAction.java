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
import org.jabref.logic.l10n.Localization;
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
        Optional<BibDatabaseContext> activeDatabaseOpt = stateManager.getActiveDatabase();
        if (activeDatabaseOpt.isEmpty()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("No library open"),
                    Localization.lang("Please open a library before pulling.")
            );
            return;
        }

        BibDatabaseContext activeDatabase = activeDatabaseOpt.get();
        Optional<Path> bibFilePathOpt = activeDatabase.getDatabasePath();
        if (bibFilePathOpt.isEmpty()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("No library file path"),
                    Localization.lang("Cannot pull from Git: No file is associated with this library.")
            );
            return;
        }

        Path bibFilePath = bibFilePathOpt.get();
        GitHandler handler = new GitHandler(bibFilePath.getParent());
        GitConflictResolverDialog dialog = new GitConflictResolverDialog(dialogService, guiPreferences);
        GitConflictResolverStrategy resolver = new GuiGitConflictResolverStrategy(dialog);
        GitSemanticMergeExecutor mergeExecutor = new GitSemanticMergeExecutorImpl(guiPreferences.getImportFormatPreferences());

        GitSyncService syncService = new GitSyncService(guiPreferences.getImportFormatPreferences(), handler, resolver, mergeExecutor);
        GitStatusViewModel statusViewModel = new GitStatusViewModel(stateManager, bibFilePath);
        GitPullViewModel viewModel = new GitPullViewModel(syncService, statusViewModel);

        BackgroundTask
                .wrap(() -> viewModel.pull())
                .onSuccess(result -> {
                    if (result.isSuccessful()) {
                        dialogService.showInformationDialogAndWait(
                                Localization.lang("Git Pull"),
                                Localization.lang("Successfully merged and updated.")
                        );
                    } else {
                        dialogService.showWarningDialogAndWait(
                                Localization.lang("Git Pull"),
                                Localization.lang("Merge completed with conflicts.")
                        );
                    }
                })
                .onFailure(ex -> {
                    if (ex instanceof JabRefException e) {
                        dialogService.showErrorDialogAndWait(
                                Localization.lang("Git Pull Failed"),
                                e.getLocalizedMessage(),
                                e
                        );
                    } else if (ex instanceof GitAPIException e) {
                        dialogService.showErrorDialogAndWait(
                                Localization.lang("Git Pull Failed"),
                                Localization.lang("An unexpected Git error occurred: %0", e.getLocalizedMessage()),
                                e
                        );
                    } else if (ex instanceof IOException e) {
                        dialogService.showErrorDialogAndWait(
                                Localization.lang("Git Pull Failed"),
                                Localization.lang("I/O error: %0", e.getLocalizedMessage()),
                                e
                        );
                    } else {
                        dialogService.showErrorDialogAndWait(
                                Localization.lang("Git Pull Failed"),
                                Localization.lang("Unexpected error: %0", ex.getLocalizedMessage()),
                                ex
                        );
                    }
                })
                .executeWith(taskExecutor);
    }
}
