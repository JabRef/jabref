package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.GitSyncService;
import org.jabref.logic.git.model.PushResult;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.errors.GitAPIException;

public class GitPushAction extends SimpleCommand {
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences guiPreferences;
    private final TaskExecutor taskExecutor;
    private final GitHandlerRegistry gitHandlerRegistry;

    public GitPushAction(DialogService dialogService,
                         StateManager stateManager,
                         GuiPreferences guiPreferences,
                         TaskExecutor taskExecutor,
                         GitHandlerRegistry handlerRegistry) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.guiPreferences = guiPreferences;
        this.taskExecutor = taskExecutor;
        this.gitHandlerRegistry = handlerRegistry;

        this.executable.bind(ActionHelper.needsGitRemoteConfigured(stateManager));
    }

    @Override
    public void execute() {
        Optional<BibDatabaseContext> activeDatabaseOpt = stateManager.getActiveDatabase();
        if (activeDatabaseOpt.isEmpty()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("No library open"),
                    Localization.lang("Please open a library before pushing.")
            );
            return;
        }

        BibDatabaseContext activeDatabase = activeDatabaseOpt.get();
        Optional<Path> bibFilePathOpt = activeDatabase.getDatabasePath();
        if (bibFilePathOpt.isEmpty()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("No library file path"),
                    Localization.lang("Cannot push to Git: No file is associated with this library.")
            );
            return;
        }

        Path bibFilePath = bibFilePathOpt.get();

        GitStatusViewModel gitStatusViewModel =
                GitStatusViewModel.fromPathAndContext(stateManager, taskExecutor, gitHandlerRegistry, bibFilePath);

        BackgroundTask
                .wrap(() -> doPush(activeDatabase, bibFilePath, gitStatusViewModel, gitHandlerRegistry))
                .onSuccess(result -> {
                    if (result.noop()) {
                        dialogService.showInformationDialogAndWait(
                                Localization.lang("Git Push"),
                                Localization.lang("Nothing to push. Local branch is up to date.")
                        );
                    } else if (result.successful()) {
                        dialogService.showInformationDialogAndWait(
                                Localization.lang("Git Push"),
                                Localization.lang("Pushed successfully.")
                        );
                    }
                })
                .onFailure(this::showPushError)
                .executeWith(taskExecutor);
    }

    private PushResult doPush(BibDatabaseContext databaseContext,
                              Path bibPath,
                              GitStatusViewModel gitStatusViewModel,
                              GitHandlerRegistry registry) throws IOException, GitAPIException, JabRefException {

        GitSyncService syncService = GitSyncService.create(guiPreferences.getImportFormatPreferences(), registry);
        GitHandler handler = registry.get(bibPath.getParent());
        String user = guiPreferences.getGitPreferences().getUsername();
        String pat = guiPreferences.getGitPreferences().getPat();
        handler.setCredentials(user, pat);

        PushResult result = syncService.push(databaseContext, bibPath);

        if (result.successful()) {
            gitStatusViewModel.refresh(bibPath);
        }
        return result;
    }

    private void showPushError(Throwable ex) {
        if (ex instanceof JabRefException e) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Git Push Failed"),
                    e.getLocalizedMessage(),
                    e
            );
        } else if (ex instanceof GitAPIException e) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Git Push Failed"),
                    Localization.lang("An unexpected Git error occurred: %0", e.getLocalizedMessage()),
                    e
            );
        } else if (ex instanceof IOException e) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Git Push Failed"),
                    Localization.lang("I/O error: %0", e.getLocalizedMessage()),
                    e
            );
        } else {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Git Push Failed"),
                    Localization.lang("Unexpected error: %0", ex.getMessage()),
                    ex
            );
        }
    }
}
