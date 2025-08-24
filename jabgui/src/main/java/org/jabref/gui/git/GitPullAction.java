package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.GitSyncService;
import org.jabref.logic.git.conflicts.GitConflictResolverStrategy;
import org.jabref.logic.git.merge.GitSemanticMergeExecutor;
import org.jabref.logic.git.merge.GitSemanticMergeExecutorImpl;
import org.jabref.logic.git.model.PullResult;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.injection.Injector;
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

        this.executable.bind(ActionHelper.needsDatabase(stateManager).and(ActionHelper.needsGitRemoteConfigured(stateManager)));
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

        GitHandlerRegistry registry = Injector.instantiateModelOrService(GitHandlerRegistry.class);
        GitStatusViewModel gitStatusViewModel = GitStatusViewModel.fromPathAndContext(stateManager, taskExecutor, registry, bibFilePath);

        BackgroundTask
                .wrap(() -> doPull(activeDatabase, bibFilePath, stateManager, registry))
                .onSuccess(result -> {
                    if (result.noop()) {
                        dialogService.showInformationDialogAndWait(
                                Localization.lang("Git Pull"),
                                Localization.lang("Already up to date.")
                        );
                    } else if (result.isSuccessful()) {
                        try {
                            replaceWithMergedEntries(result.getMergedEntries(), activeDatabase);
                            gitStatusViewModel.refresh(bibFilePath);
                            dialogService.showInformationDialogAndWait(
                                    Localization.lang("Git Pull"),
                                    Localization.lang("Merged and updated."));
                        } catch (IOException | JabRefException ex) {
                            showPullError(ex);
                        }
                    }
                })
                .onFailure(exception -> showPullError(exception))
                .executeWith(taskExecutor);
    }

    private PullResult doPull(BibDatabaseContext databaseContext, Path bibPath, StateManager stateManager, GitHandlerRegistry registry) throws IOException, GitAPIException, JabRefException {
        GitSyncService syncService = buildSyncService(bibPath, registry);
        GitHandler handler = registry.get(bibPath.getParent());
        String user = guiPreferences.getGitPreferences().getUsername();
        String pat = guiPreferences.getGitPreferences().getPat();
        handler.setCredentials(user, pat);
        return syncService.fetchAndMerge(databaseContext, bibPath);
    }

    private GitSyncService buildSyncService(Path bibPath, GitHandlerRegistry handlerRegistry) throws JabRefException {
        GitConflictResolverDialog dialog = new GitConflictResolverDialog(dialogService, guiPreferences);
        GitConflictResolverStrategy resolver = new GuiGitConflictResolverStrategy(dialog);
        GitSemanticMergeExecutor mergeExecutor = new GitSemanticMergeExecutorImpl(guiPreferences.getImportFormatPreferences());

        return new GitSyncService(guiPreferences.getImportFormatPreferences(), handlerRegistry, resolver, mergeExecutor);
    }

    private void showPullError(Throwable exception) {
        if (exception instanceof JabRefException e) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Git Pull Failed"),
                    e.getLocalizedMessage(),
                    e
            );
        } else if (exception instanceof GitAPIException e) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Git Pull Failed"),
                    Localization.lang("An unexpected Git error occurred: %0", e.getLocalizedMessage()),
                    e
            );
        } else if (exception instanceof IOException e) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Git Pull Failed"),
                    Localization.lang("I/O error: %0", e.getLocalizedMessage()),
                    e
            );
        } else {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Git Pull Failed"),
                    Localization.lang("Unexpected error: %0", exception.getLocalizedMessage()),
                    exception
            );
        }
    }

    private void replaceWithMergedEntries(List<BibEntry> mergedEntries, BibDatabaseContext databaseContext) throws IOException, JabRefException {
        List<BibEntry> currentEntries = new ArrayList<>(databaseContext.getDatabase().getEntries());
        for (BibEntry entry : currentEntries) {
            databaseContext.getDatabase().removeEntry(entry);
        }

        for (BibEntry entry : mergedEntries) {
            databaseContext.getDatabase().insertEntry(new BibEntry(entry));
        }
    }
}
