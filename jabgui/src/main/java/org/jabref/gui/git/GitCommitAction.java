package org.jabref.gui.git;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.l10n.Localization;

public class GitCommitAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;

    public GitCommitAction(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        if (hasNothingToCommit()) {
            dialogService.notify(Localization.lang("Nothing to commit."));
            return;
        }

        dialogService.showCustomDialogAndWait(
                new GitCommitDialogView()
        );
    }

    private boolean hasNothingToCommit() {
        return stateManager.getActiveDatabase()
                           .flatMap(context -> context.getDatabasePath())
                           .flatMap(GitHandler::fromAnyPath)
                           .map(GitStatusChecker::checkStatus)
                           .map(status -> !status.uncommittedChanges())
                           .orElse(true);
    }
}
