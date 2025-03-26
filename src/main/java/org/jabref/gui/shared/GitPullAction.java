package org.jabref.gui.shared;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.git.GitClientHandler;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitPullAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitPullAction.class);

    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private final StateManager stateManager;

    public GitPullAction(
            GuiPreferences preferences,
            DialogService dialogService,
            StateManager stateManager) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
    }
    /**
     * Contains logic for performing Git operations on the active database repository.
     * The method verifies that an active database exists and is not empty
     * then creates a GitClientHandler with the parent directory of the database path.
     * If the directory is a Git repository, performs a pull operation on the current branch.
     * Will log any errors that occur during the pull operation.
     */

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().get();

        Optional<Path> path = databaseContext.getDatabasePath();
        if (path.isEmpty()) {
            return;
        }

        GitClientHandler gitClientHandler = new GitClientHandler(path.get().getParent(),
                dialogService,
                preferences);
        try {
            gitClientHandler.checkGitRepoAndPullAndDisplayMsg();
        } catch (IOException e) {
        LOGGER.error("Error while pulling and displaying message", e);
        dialogService.showErrorDialogAndWait(Localization.lang("Git Pull Error"),
                Localization.lang("Fail to pull changes: {0}", e.getMessage()));
        }
    }
}
