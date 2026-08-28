package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.errors.GitAPIException;

public class GitCommitAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GitHandlerRegistry gitHandlerRegistry;

    public GitCommitAction(DialogService dialogService, StateManager stateManager, GitHandlerRegistry gitHandlerRegistry) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.gitHandlerRegistry = gitHandlerRegistry;

        this.executable.bind(ActionHelper.needsSavedLocalDatabase(stateManager));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase()
                    .flatMap(BibDatabaseContext::getDatabasePath)
                    .ifPresent(this::commit);
    }

    private void commit(Path bibFilePath) {
        if (GitHandler.findRepositoryRoot(bibFilePath).isEmpty()) {
            initRepository(bibFilePath);
            return;
        }

        if (hasNothingToCommit(bibFilePath)) {
            dialogService.notify(Localization.lang("Nothing to commit."));
            return;
        }

        dialogService.showCustomDialogAndWait(
                new GitCommitDialogView()
        );
    }

    /// Offers to put a library that is not under version control into a fresh repository.
    /// Cancelling is a valid choice: the user may want to clone an existing repository into that folder instead.
    private void initRepository(Path bibFilePath) {
        Path directory = bibFilePath.toAbsolutePath().getParent();
        boolean initialize = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Git Commit"),
                Localization.lang("This library is not under Git version control.\nInitialize a Git repository in %0 and commit %1?\nOther files in that folder stay untracked.", directory.toString(), bibFilePath.getFileName().toString()),
                Localization.lang("Initialize"),
                Localization.lang("Cancel"));
        if (!initialize) {
            return;
        }

        try {
            gitHandlerRegistry.get(directory).initAndCommit(bibFilePath);
            dialogService.notify(Localization.lang("Initialized Git repository in %0", directory.toString()));
        } catch (IOException | GitAPIException e) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Git Commit"),
                    Localization.lang("Could not initialize a Git repository in %0", directory.toString()),
                    e);
        }
    }

    private boolean hasNothingToCommit(Path bibFilePath) {
        return gitHandlerRegistry.fromAnyPath(bibFilePath)
                                 .map(GitStatusChecker::checkStatus)
                                 .map(status -> !status.uncommittedChanges())
                                 .orElse(true);
    }
}
