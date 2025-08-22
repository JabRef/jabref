package org.jabref.gui.git;

import javafx.beans.binding.BooleanExpression;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class GitShareToGitHubAction extends SimpleCommand {
    private final DialogService dialogService;
    private final StateManager stateManager;

    public GitShareToGitHubAction(
            DialogService dialogService,
            StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(this.enabledGitShare());
    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new GitShareToGitHubDialogView());
    }

    private BooleanExpression enabledGitShare() {
        // TODO: Determine the correct condition for enabling "Git Share". This currently only requires an open library.
        //  In the future, this may need to check whether:
        //  - the repo is initialized (because without a repository, the current implementation does not work -> future work)
        //  - etc.
        // Can be called independent if a remote is configured or not -- it will be done in the dialog
        // HowTo: Inject the observables (or maybe the stateManager) containing these constraints
        return needsDatabase(stateManager);
    }
}
