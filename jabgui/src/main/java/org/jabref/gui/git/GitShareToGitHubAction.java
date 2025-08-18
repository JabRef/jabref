package org.jabref.gui.git;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.git.prefs.GitPreferences;
import org.jabref.logic.util.TaskExecutor;

public class GitShareToGitHubAction extends SimpleCommand {
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final GitPreferences gitPreferences;
    private final TaskExecutor taskExecutor;

    public GitShareToGitHubAction(DialogService dialogService, StateManager stateManager, ExternalApplicationsPreferences externalApplicationsPreferences, GitPreferences gitPreferences, TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.gitPreferences = gitPreferences;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new GitShareToGitHubDialogView(stateManager, dialogService, taskExecutor, externalApplicationsPreferences, gitPreferences));
    }
}
