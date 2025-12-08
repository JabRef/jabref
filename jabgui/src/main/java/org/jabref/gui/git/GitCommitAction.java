package org.jabref.gui.git;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

public class GitCommitAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GitPreferences gitPreferences;

    public GitCommitAction(DialogService dialogService, StateManager stateManager, GitPreferences gitPreferences) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.gitPreferences = gitPreferences;

        this.executable.bind(ActionHelper.needsSavedLocalDatabase(stateManager));
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
                           .flatMap(BibDatabaseContext::getDatabasePath)
                           .flatMap(path -> GitHandler.fromAnyPath(path, gitPreferences))
                           .map(GitStatusChecker::checkStatus)
                           .map(status -> !status.uncommittedChanges())
                           .orElse(true);
    }
}
