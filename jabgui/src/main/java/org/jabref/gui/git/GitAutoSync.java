package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.GitSyncService;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.io.GitFileWriter;
import org.jabref.logic.git.model.BookkeepingResult;
import org.jabref.logic.git.model.PullPlan;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.logic.git.merge.execution.GitMergeApplier.applyAutoPlan;
import static org.jabref.logic.git.merge.execution.GitMergeApplier.applyResolved;

/// Runs the Git operations configured in the library properties.
///
/// Same operations as [GitCommitAction] and [GitPushAction], but started by a save or a timer
/// instead of the user. Unattended operations report failures as notifications. a conflict on the
/// pull before an auto-push prompts, because the user started that operation by saving
@NullMarked
public class GitAutoSync {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitAutoSync.class);

    private final DialogService dialogService;
    private final GitHandlerRegistry gitHandlerRegistry;
    private final TaskExecutor taskExecutor;
    private final GuiPreferences guiPreferences;
    private final StateManager stateManager;

    public GitAutoSync(DialogService dialogService,
                       GitHandlerRegistry gitHandlerRegistry,
                       TaskExecutor taskExecutor,
                       GuiPreferences guiPreferences,
                       StateManager stateManager) {
        this.dialogService = dialogService;
        this.gitHandlerRegistry = gitHandlerRegistry;
        this.taskExecutor = taskExecutor;
        this.guiPreferences = guiPreferences;
        this.stateManager = stateManager;
    }

    /// Commits the library if it is inside a Git repository, and pushes afterwards if requested.
    /// does nothing if the repository is clean
    public void commit(Path bibFilePath, BibDatabaseContext databaseContext, boolean pushAfterCommit) {
        gitHandlerRegistry.fromAnyPath(bibFilePath).ifPresent(gitHandler ->
                BackgroundTask.wrap(() -> doCommit(gitHandler))
                              .onSuccess(committed -> {
                                  if (committed && pushAfterCommit) {
                                      pullThenPush(bibFilePath, databaseContext);
                                  }
                              })
                              .onFailure(this::showCommitError)
                              .executeWith(taskExecutor));
    }

    private boolean doCommit(GitHandler gitHandler) throws JabRefException, GitAPIException, IOException {
        if (GitStatusChecker.checkStatus(gitHandler).conflict()) {
            throw new JabRefException(Localization.lang("Commit aborted: Local repository has unresolved merge conflicts."));
        }
        return gitHandler.createCommitOnCurrentBranch(Localization.lang("Update references"), false);
    }

    private void pullThenPush(Path bibFilePath, BibDatabaseContext databaseContext) {
        BackgroundTask.wrap(() -> syncService().prepareMerge(databaseContext, bibFilePath))
                      .onSuccess(pullPlan -> {
                          if (pullPlan.isEmpty()) {
                              push(bibFilePath, databaseContext);
                              return;
                          }
                          applyMerge(bibFilePath, databaseContext, pullPlan.get());
                      })
                      .onFailure(this::showPullError)
                      .executeWith(taskExecutor);
    }

    private void applyMerge(Path bibFilePath, BibDatabaseContext databaseContext, PullPlan pullPlan) {
        List<ThreeWayEntryConflict> conflicts = pullPlan.conflicts();
        if (!conflicts.isEmpty()) {
            if (!dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Git Pull"),
                    Localization.lang("Detected conflicting changes during pull."),
                    Localization.lang("Resolve conflicts"),
                    Localization.lang("Cancel"))) {
                dialogService.notify(Localization.lang("Pull canceled."));
                return;
            }
            List<BibEntry> resolved = new GuiGitConflictResolverStrategy(
                    new GitConflictResolverDialog(dialogService, guiPreferences, stateManager)).resolveConflicts(conflicts);
            if (resolved.isEmpty()) {
                dialogService.notify(Localization.lang("Pull canceled."));
                return;
            }
            applyAutoPlan(databaseContext, pullPlan.autoPlan());
            applyResolved(databaseContext, resolved);
        } else {
            applyAutoPlan(databaseContext, pullPlan.autoPlan());
        }

        writeAndFinalize(bibFilePath, databaseContext, pullPlan)
                .onSuccess(_ -> push(bibFilePath, databaseContext))
                .onFailure(this::showPullError)
                .executeWith(taskExecutor);
    }

    private BackgroundTask<BookkeepingResult> writeAndFinalize(Path bibFilePath, BibDatabaseContext databaseContext, PullPlan pullPlan) {
        return BackgroundTask.wrap(() -> {
            GitFileWriter.write(bibFilePath, databaseContext, guiPreferences.getImportFormatPreferences());
            return syncService().finalizeMerge(bibFilePath, pullPlan);
        });
    }

    /// Pulls remote changes if the library is inside a Git repository. Skips quietly when there are
    /// uncommitted changes or the merge has conflicts, because no user started this operation.
    public void pull(Path bibFilePath, BibDatabaseContext databaseContext) {
        gitHandlerRegistry.fromAnyPath(bibFilePath).ifPresent(gitHandler ->
                BackgroundTask.wrap(() -> preparePull(gitHandler, databaseContext, bibFilePath))
                              .onSuccess(pullPlan -> pullPlan.ifPresent(plan -> applyCleanMerge(bibFilePath, databaseContext, plan)))
                              .onFailure(this::showPullError)
                              .executeWith(taskExecutor));
    }

    private Optional<PullPlan> preparePull(GitHandler gitHandler, BibDatabaseContext databaseContext, Path bibFilePath)
            throws JabRefException, GitAPIException, IOException {
        if (GitStatusChecker.checkStatus(gitHandler).uncommittedChanges()) {
            return Optional.empty();
        }
        return syncService().prepareMerge(databaseContext, bibFilePath);
    }

    private void applyCleanMerge(Path bibFilePath, BibDatabaseContext databaseContext, PullPlan pullPlan) {
        if (!pullPlan.conflicts().isEmpty()) {
            dialogService.notify(Localization.lang("Detected conflicting changes during pull."));
            return;
        }
        applyAutoPlan(databaseContext, pullPlan.autoPlan());
        writeAndFinalize(bibFilePath, databaseContext, pullPlan)
                .onFailure(this::showPullError)
                .executeWith(taskExecutor);
    }

    private GitSyncService syncService() {
        return GitSyncService.create(guiPreferences.getImportFormatPreferences(), gitHandlerRegistry);
    }

    private void push(Path bibFilePath, BibDatabaseContext databaseContext) {
        BackgroundTask.wrap(() -> syncService().push(databaseContext, bibFilePath))
                      .onFailure(this::showPushError)
                      .executeWith(taskExecutor);
    }

    private void showPullError(Exception exception) {
        LOGGER.warn("Automatic Git pull failed", exception);
        dialogService.notify(Localization.lang("Git Pull Failed") + ": " + exception.getLocalizedMessage());
    }

    private void showPushError(Exception exception) {
        LOGGER.warn("Automatic Git push failed", exception);
        dialogService.notify(Localization.lang("Git Push Failed") + ": " + exception.getLocalizedMessage());
    }

    private void showCommitError(Exception exception) {
        LOGGER.warn("Automatic Git commit failed", exception);
        dialogService.notify(Localization.lang("Git Commit Failed") + ": " + exception.getLocalizedMessage());
    }
}
