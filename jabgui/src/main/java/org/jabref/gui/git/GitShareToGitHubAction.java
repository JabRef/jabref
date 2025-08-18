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
        // TODO: Determine the correct condition for enabling "Git Share". This currently only requires an open database.
        //  In the future, this may need to check whether:
        //  - the repo is initialized
        //  - the remote is not already configured, or needs to be reset
        //  - etc.
        return needsDatabase(stateManager);
    }
}
