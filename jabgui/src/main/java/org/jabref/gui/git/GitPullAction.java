package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
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
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.io.GitFileWriter;
import org.jabref.logic.git.model.BookkeepingResult;
import org.jabref.logic.git.model.MergePlan;
import org.jabref.logic.git.model.PullPlan;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.eclipse.jgit.api.errors.GitAPIException;

public class GitPullAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences guiPreferences;
    private final TaskExecutor taskExecutor;
    private final GitHandlerRegistry gitHandlerRegistry;

    public GitPullAction(DialogService dialogService,
                         StateManager stateManager,
                         GuiPreferences guiPreferences,
                         TaskExecutor taskExecutor,
                         GitHandlerRegistry gitHandlerRegistry) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.guiPreferences = guiPreferences;
        this.taskExecutor = taskExecutor;
        this.gitHandlerRegistry = gitHandlerRegistry;

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

        GitStatusViewModel gitStatusViewModel = GitStatusViewModel.fromPathAndContext(stateManager, taskExecutor, gitHandlerRegistry, bibFilePath);

        BackgroundTask
                .wrap(() -> prepareMergeResult(activeDatabase, bibFilePath, gitHandlerRegistry))
                .onSuccess(pullComputation -> {
                    if (pullComputation.isNoop()) {
                        dialogService.showInformationDialogAndWait(
                                Localization.lang("Git Pull"),
                                Localization.lang("Already up to date.")
                        );
                        return;
                    }
                    if (pullComputation.isNoopAhead()) {
                        dialogService.showInformationDialogAndWait(
                                Localization.lang("Git Pull"),
                                Localization.lang("Local branch is ahead of remote. No changes were made.")
                        );
                        return;
                    }

                    MergePlan autoMergePlan = pullComputation.autoPlan();
                    List<ThreeWayEntryConflict> conflicts = pullComputation.conflicts();

                    int autoNewCount = autoMergePlan.newEntries().size();
                    int autoModifiedCount = autoMergePlan.fieldPatches().size();
                    int autoDeletedCount = autoMergePlan.deletedEntryKeys().size();

                    applyAutoPlan(activeDatabase, autoMergePlan);

                    int manualResolvedCount;
                    if (!conflicts.isEmpty()) {
                        // resolve via GUI (strategy jumps to FX thread internally; safe to call from background)
                        GitConflictResolverStrategy resolver = new GuiGitConflictResolverStrategy(new GitConflictResolverDialog(dialogService, guiPreferences));
                        List<BibEntry> resolved = resolver.resolveConflicts(conflicts);
                        if (resolved.isEmpty()) {
                            dialogService.notify(Localization.lang("Pull canceled."));
                            return;
                        }
                        manualResolvedCount = resolved.size();
                        applyResolved(activeDatabase, resolved);
                    } else {
                        manualResolvedCount = 0;
                    }

                    BackgroundTask.wrap(() -> saveAndFinalize(bibFilePath, activeDatabase, pullComputation))
                                  .onSuccess(finalizeResult -> {
                                      gitStatusViewModel.refresh(bibFilePath);
                                      if (finalizeResult.isFastForward()) {
                                          dialogService.showInformationDialogAndWait(
                                                  Localization.lang("Git Pull"),
                                                  Localization.lang("Fast-forwarded to remote.")
                                          );
                                      } else {
                                          String stats = Localization.lang(
                                                  "Auto-applied changes: %0 new, %1 modified, %2 deleted.",
                                                  Integer.toString(autoNewCount),
                                                  Integer.toString(autoModifiedCount),
                                                  Integer.toString(autoDeletedCount)
                                          );
                                          if (manualResolvedCount > 0) {
                                              stats = stats + " " + Localization.lang(
                                                      "%0 conflicts resolved.",
                                                      Integer.toString(manualResolvedCount)
                                              );
                                          }

                                          dialogService.showInformationDialogAndWait(
                                                  Localization.lang("Git Pull"),
                                                  Localization.lang("Merged and updated.") + " " + stats
                                          );
                                      }
                                  })
                                  .onFailure(this::showPullError)
                                  .executeWith(taskExecutor);
                })
                .onFailure(this::showPullError)
                .executeWith(taskExecutor);
    }

    private PullPlan prepareMergeResult(BibDatabaseContext databaseContext, Path bibPath, GitHandlerRegistry registry) throws IOException, GitAPIException, JabRefException {
        GitSyncService gitSyncService = GitSyncService.create(guiPreferences.getImportFormatPreferences(), registry);
        GitHandler handler = registry.get(bibPath.getParent());
        String user = guiPreferences.getGitPreferences().getUsername();
        String pat = guiPreferences.getGitPreferences().getPat();
        handler.setCredentials(user, pat);
        return gitSyncService.prepareMerge(databaseContext, bibPath);
    }

    private BookkeepingResult saveAndFinalize(Path bibPath,
                                              BibDatabaseContext databaseContext,
                                              PullPlan pullPlan)
            throws IOException, GitAPIException, JabRefException {
        GitFileWriter.write(bibPath, databaseContext, guiPreferences.getImportFormatPreferences());
        // Git bookkeeping
        GitSyncService gitSyncService = GitSyncService.create(guiPreferences.getImportFormatPreferences(), gitHandlerRegistry);
        return gitSyncService.finalizeMerge(bibPath, pullPlan);
    }

    // ------------------- helpers: memory mutations -------------------

    /// Apply (remote - base) patches safely into the in-memory DB, plus safe new/deleted entries.
    private static void applyAutoPlan(BibDatabaseContext bibDatabaseContext, MergePlan plan) {
        // new entries
        for (BibEntry entry : plan.newEntries()) {
            bibDatabaseContext.getDatabase().insertEntry(new BibEntry(entry));
        }
        // field patches (null means delete field)
        plan.fieldPatches().forEach((key, patch) ->
                bibDatabaseContext.getDatabase().getEntryByCitationKey(key).ifPresent(entry -> {
                    patch.forEach((field, newValue) -> {
                        if (newValue == null) {
                            entry.clearField(field);
                        } else {
                            entry.setField(field, newValue);
                        }
                    });
                })
        );
        // deletions that are semantically safe (local kept base)
        for (String key : plan.deletedEntryKeys()) {
            bibDatabaseContext.getDatabase().getEntryByCitationKey(key).ifPresent(e -> bibDatabaseContext.getDatabase().removeEntry(e));
        }
    }

    /**
     * Apply user-resolved entries into MEMORY: replace or insert by citation key.
     * (Aligned with MergeEntriesAction’s “edit the in-memory database first” philosophy.)
     */
    private static void applyResolved(BibDatabaseContext bibDatabaseContext, List<BibEntry> resolved) {
        for (BibEntry merged : resolved) {
            merged.getCitationKey().ifPresent(key -> {
                bibDatabaseContext.getDatabase().getEntryByCitationKey(key).ifPresentOrElse(existing -> {
                    existing.setType(merged.getType());
                    existing.getFields().forEach(field -> {
                        if (merged.getField(field).isEmpty()) {
                            existing.clearField(field);
                        }
                    });
                    merged.getFields().forEach(field -> merged.getField(field).ifPresent(value -> existing.setField(field, value)));
                }, () -> bibDatabaseContext.getDatabase().insertEntry(new BibEntry(merged)));
            });
        }
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
}
