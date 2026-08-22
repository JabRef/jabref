package org.jabref.gui.git;

import javafx.beans.binding.BooleanExpression;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

public class GitShareToGitHubAction extends SimpleCommand {
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GitPreferences gitPreferences;

    public GitShareToGitHubAction(
            DialogService dialogService,
            StateManager stateManager,
            GitPreferences gitPreferences) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.gitPreferences = gitPreferences;

        this.executable.bind(this.enabledGitShare());
    }

    @Override
    public void execute() {
        if (StringUtil.isBlank(gitPreferences.getRepositoryUrl())) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Share this Library to GitHub"),
                    Localization.lang("No repository URL. Please configure it in the preferences."));
            return;
        }
        dialogService.showCustomDialogAndWait(new GitShareToGitHubDialogView());
    }

    private BooleanExpression enabledGitShare() {
        // TODO: Revisit this condition once the sharing dialog reads the active library's configured remote
        return ActionHelper.needsSavedLocalDatabase(stateManager);
    }
}
