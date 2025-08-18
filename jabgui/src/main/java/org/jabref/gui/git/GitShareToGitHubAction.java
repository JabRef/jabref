package org.jabref.gui.git;

import javafx.beans.binding.BooleanExpression;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.util.TaskExecutor;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class GitShareToGitHubAction extends SimpleCommand {
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final GitPreferences gitPreferences;
    private final TaskExecutor taskExecutor;

    public GitShareToGitHubAction(
            DialogService dialogService,
            StateManager stateManager,
            ExternalApplicationsPreferences externalApplicationsPreferences,
            GitPreferences gitPreferences,
            TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.gitPreferences = gitPreferences;
        this.taskExecutor = taskExecutor;

        this.executable.bind(this.enabledGitShare());
    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new GitShareToGitHubDialogView(
                stateManager,
                dialogService,
                taskExecutor,
                externalApplicationsPreferences,
                gitPreferences));
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
