package org.jabref.gui.git;

import javafx.beans.binding.BooleanExpression;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

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
        // TODO: Revisit this condition once the sharing dialog reads the active library's configured remote 
        return ActionHelper.needsSavedLocalDatabase(stateManager);
    }
}
