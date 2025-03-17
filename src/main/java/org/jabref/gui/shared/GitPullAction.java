package org.jabref.gui.shared;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.git.GitHandler;
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

    @Override
    public void execute() {
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().get();
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        Optional<Path> path = databaseContext.getDatabasePath();
        if (path.isEmpty()) {
            return;
        }

        GitHandler gitHandler = new GitHandler(path.get().getParent(), false);
        if (gitHandler.isGitRepository()) {
            try {
                gitHandler.updateCredentials(preferences.getGitPreferences());
                gitHandler.pullOnCurrentBranch();
            } catch (Exception e) {
                dialogService.showErrorDialogAndWait(e);
            }
        } else {
            LOGGER.info(String.valueOf(path.get()));
            LOGGER.info("Not a git repository");
        }
    }
}
