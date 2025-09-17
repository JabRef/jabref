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
import org.jabref.logic.git.merge.DefaultMergeBookkeeper;
import org.jabref.logic.git.merge.GitSemanticMergePlanner;
import org.jabref.logic.git.model.FinalizeResult;
import org.jabref.logic.git.model.MergePlan;
import org.jabref.logic.git.model.PullComputation;
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
                .wrap(() -> doPull(activeDatabase, bibFilePath, gitHandlerRegistry))
                .onSuccess(fr -> {
                    //                    // 刷新 Git 状态栏
                    //                    gitStatusViewModel.refresh(bibFilePath);

                    if (fr.isUpToDate()) {
                        dialogService.showInformationDialogAndWait(
                                Localization.lang("Git Pull"),
                                Localization.lang("Already up to date.")
                        );
                        return;
                    }
                    if (fr.isAhead()) {
                        dialogService.showInformationDialogAndWait(
                                Localization.lang("Git Pull"),
                                Localization.lang("Local branch is ahead of remote. No changes were made.")
                        );
                        return;
                    }

                    if (fr.isFastForward()) {
                        dialogService.showInformationDialogAndWait(
                                Localization.lang("Git Pull"),
                                Localization.lang("Fast-forwarded to remote.")
                        );
                        return;
                    }

                    if (fr.hasNewCommit()) {
                        dialogService.showInformationDialogAndWait(
                                Localization.lang("Git Pull"),
                                Localization.lang("Merged and recorded as commit %0.", fr.commitId().orElse("-"))
                        );
                    }
                })
                .onFailure(this::showPullError)
                .executeWith(taskExecutor);
        //                .onSuccess(result -> {
        //                    if (result.noop()) {
        //                        dialogService.showInformationDialogAndWait(
        //                                Localization.lang("Git Pull"),
        //                                Localization.lang("Already up to date.")
        //                        );
        //                    } else if (result.isSuccessful()) {
        //                        try {
        //                            replaceWithMergedEntries(result.getMergedEntries(), activeDatabase);
        //                            gitStatusViewModel.refresh(bibFilePath);
        //                            dialogService.showInformationDialogAndWait(
        //                                    Localization.lang("Git Pull"),
        //                                    Localization.lang("Merged and updated."));
        //                        } catch (IOException | JabRefException ex) {
        //                            showPullError(ex);
        //                        }
        //                    }
        //                })
    }

    private FinalizeResult doPull(BibDatabaseContext databaseContext, Path bibPath, GitHandlerRegistry registry) throws IOException, GitAPIException, JabRefException {
        GitSyncService gitSyncService = buildSyncService(registry);
        GitHandler handler = registry.get(bibPath.getParent());
        String user = guiPreferences.getGitPreferences().getUsername();
        String pat = guiPreferences.getGitPreferences().getPat();
        handler.setCredentials(user, pat);

        // 1. prepare merge result
        PullComputation pullComputation = gitSyncService.prepareMerge(databaseContext, bibPath);
        if (pullComputation.isNoop()) {
            return FinalizeResult.upToDate();
        }
        if (pullComputation.isNoopAhead()) {
            return FinalizeResult.ahead();
        }

        // 2. APPLY TO MEMORY
        MergePlan autoMergePlan = pullComputation.autoPlan();
        List<ThreeWayEntryConflict> conflicts = pullComputation.conflicts();
        applyAutoPlan(databaseContext, autoMergePlan);

        if (!conflicts.isEmpty()) {
            // resolve via GUI (strategy jumps to FX thread internally; safe to call from background)
            GitConflictResolverStrategy resolver = new GuiGitConflictResolverStrategy(
                    new GitConflictResolverDialog(dialogService, guiPreferences));
            List<BibEntry> resolved = resolver.resolveConflicts(conflicts);
            if (resolved.isEmpty()) {
                throw new JabRefException("Pull canceled: conflict resolution aborted.");
            }
            applyResolved(databaseContext, resolved);
        }

        // 3. SAVE MEMORY → DISK ONCE
        // TODO: replace this with the exact SaveDatabaseAction pipeline and temporarily suppress
        saveLikeUserSave(bibPath, databaseContext);

        // 4. Git bookkeeping
        FinalizeResult finalizeResult = gitSyncService.finalizeMerge(bibPath, pullComputation);
        return gitSyncService.finalizeMerge(bibPath, pullComputation);
    }

    // ------------------- helpers: memory mutations -------------------

    /**
     * Apply (remote - base) patches safely into the in-memory DB, plus safe new/deleted entries.
     * We intentionally mutate MEMORY first so we can run the same save path as a manual save afterwards.
     */
    private static void applyAutoPlan(BibDatabaseContext ctx, MergePlan plan) {
        // new entries
        for (BibEntry e : plan.newEntries()) {
            ctx.getDatabase().insertEntry(new BibEntry(e));
        }
        // field patches (null means delete field)
        plan.fieldPatches().forEach((key, patch) ->
                ctx.getDatabase().getEntryByCitationKey(key).ifPresent(entry -> {
                    patch.forEach((field, newVal) -> {
                        if (newVal == null) {
                            entry.clearField(field);
                        } else {
                            entry.setField(field, newVal);
                        }
                    });
                })
        );
        // deletions that are semantically safe (local kept base)
        for (String key : plan.deletedEntryKeys()) {
            ctx.getDatabase().getEntryByCitationKey(key).ifPresent(e -> ctx.getDatabase().removeEntry(e));
        }
    }

    /**
     * Apply user-resolved entries into MEMORY: replace or insert by citation key.
     * (Aligned with MergeEntriesAction’s “edit the in-memory database first” philosophy.)
     */
    private static void applyResolved(BibDatabaseContext ctx, List<BibEntry> resolved) {
        for (BibEntry merged : resolved) {
            merged.getCitationKey().ifPresent(key -> {
                ctx.getDatabase().getEntryByCitationKey(key).ifPresentOrElse(existing -> {
                    // Replace content of existing entry with the resolved one
                    existing.setType(merged.getType());
                    // Clear fields that disappeared; then copy all fields from resolved
                    existing.getFields().forEach(f -> {
                        if (merged.getField(f).isEmpty()) {
                            existing.clearField(f);
                        }
                    });
                    merged.getFields().forEach(f -> merged.getField(f).ifPresent(v -> existing.setField(f, v)));
                }, () -> ctx.getDatabase().insertEntry(new BibEntry(merged)));
            });
        }
    }

    // ------------------- helpers: save & service wiring -------------------

    /**
     * Minimal save aligned with manual save behavior.
     * Later you can swap this to the full SaveDatabaseAction pipeline to inherit formatting/backup/undo notifications,
     * and wrap it with a “suppress external change detection” guard during this single write.
     */
    private void saveLikeUserSave(Path file, BibDatabaseContext ctx) throws IOException {
        org.jabref.logic.git.io.GitFileWriter.write(file, ctx, guiPreferences.getImportFormatPreferences());
    }

    /**
     * Wire a GitSyncService that does NOT write bytes in prepare/finalize;
     * GUI owns the write; finalize only records commit parents/FF via MergeBookkeeper.
     */
    private GitSyncService buildSyncService(GitHandlerRegistry handlerRegistry) {
        // mergePlanner no longer used along this path; pass null (or remove from ctor if you refactored it)
        GitSemanticMergePlanner unused = null;
        return new GitSyncService(
                guiPreferences.getImportFormatPreferences(),
                handlerRegistry,
                // resolver is created ad-hoc when needed; passing null here is fine
                /* gitConflictResolverStrategy = */ null,
                unused,
                new DefaultMergeBookkeeper(handlerRegistry)
        );
    }
    //
    //    // TODO: 看一下这个职责应该给谁；检查参数
    //    private GitSyncService buildSyncService(Path bibPath, GitHandlerRegistry handlerRegistry) throws JabRefException {
    //        GitConflictResolverDialog dialog = new GitConflictResolverDialog(dialogService, guiPreferences);
    //        GitConflictResolverStrategy resolver = new GuiGitConflictResolverStrategy(dialog);
    //        GitSemanticMergePlanner mergeExecutor = new GitSemanticMergeExecutorImpl(guiPreferences.getImportFormatPreferences());
    //
    //        return new GitSyncService(guiPreferences.getImportFormatPreferences(), handlerRegistry, resolver, mergeExecutor);
    //    }

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
