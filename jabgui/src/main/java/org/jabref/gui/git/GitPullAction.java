package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.JabRefException;
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

import static org.jabref.logic.git.merge.execution.GitMergeApplier.applyAutoPlan;
import static org.jabref.logic.git.merge.execution.GitMergeApplier.applyResolved;

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

        this.executable.bind(ActionHelper.needsGitRemoteConfigured(stateManager));
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
                .onSuccess(pullPlanOpt -> {
                    if (pullPlanOpt.isEmpty()) {
                        dialogService.showInformationDialogAndWait(
                                Localization.lang("Git Pull"),
                                Localization.lang("Already up to date or local branch is ahead.")
                        );
                        return;
                    }

                    PullPlan pullPlan = pullPlanOpt.get();
                    MergePlan autoMergePlan = pullPlan.autoPlan();
                    List<ThreeWayEntryConflict> conflicts = pullPlan.conflicts();

                    int autoNewCount = autoMergePlan.newEntries().size();
                    int autoModifiedCount = autoMergePlan.fieldPatches().size();
                    int autoDeletedCount = autoMergePlan.deletedEntryKeys().size();

                    applyAutoPlan(activeDatabase, autoMergePlan);

                    int manualResolvedCount;
                    if (!conflicts.isEmpty()) {
                        // resolve via GUI (strategy jumps to FX thread internally; safe to call from background)
                        GitConflictResolverStrategy resolver = new GuiGitConflictResolverStrategy(new GitConflictResolverDialog(dialogService, guiPreferences, stateManager));
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

                    BackgroundTask.wrap(() -> saveAndFinalize(bibFilePath, activeDatabase, pullPlan))
                                  .onSuccess(finalizeResult -> {
                                      gitStatusViewModel.refresh(bibFilePath);
                                      if (finalizeResult.isFastForward()) {
                                          dialogService.showInformationDialogAndWait(
                                                  Localization.lang("Git Pull"),
                                                  Localization.lang("Fast-forwarded to remote.")
                                          );
                                      } else {
                                          StringJoiner joiner = new StringJoiner(" ");
                                          joiner.add(Localization.lang(
                                                  "Auto-applied changes: %0 new, %1 modified, %2 deleted.",
                                                  String.valueOf(autoNewCount),
                                                  String.valueOf(autoModifiedCount),
                                                  String.valueOf(autoDeletedCount)
                                          ));
                                          if (manualResolvedCount > 0) {
                                              joiner.add(Localization.lang(
                                                      "%0 conflicts resolved.",
                                                      String.valueOf(manualResolvedCount)
                                              ));
                                          }
                                          String stats = joiner.toString();

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

    /// Prepares a merge plan for the given library and file path.
    ///
    /// @return An Optional containing the PullPlan if a merge is needed,
    ///         or Optional.empty() if the local library is already up-to-date or ahead of the remote branch.
    private Optional<PullPlan> prepareMergeResult(BibDatabaseContext databaseContext, Path bibPath, GitHandlerRegistry registry) throws IOException, GitAPIException, JabRefException {
        GitSyncService gitSyncService = GitSyncService.create(guiPreferences.getImportFormatPreferences(), registry);
        return gitSyncService.prepareMerge(databaseContext, bibPath);
    }

    private BookkeepingResult saveAndFinalize(Path bibPath, BibDatabaseContext databaseContext, PullPlan pullPlan) throws IOException, GitAPIException, JabRefException {
        GitFileWriter.write(bibPath, databaseContext, guiPreferences.getImportFormatPreferences());
        GitSyncService gitSyncService = GitSyncService.create(guiPreferences.getImportFormatPreferences(), gitHandlerRegistry);
        return gitSyncService.finalizeMerge(bibPath, pullPlan);
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
