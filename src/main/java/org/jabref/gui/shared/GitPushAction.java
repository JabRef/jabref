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

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitPushAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitPushAction.class);

    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private final StateManager stateManager;

    public GitPushAction(
            GuiPreferences preferences,
            DialogService dialogService,
            StateManager stateManager) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
    }

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
        if (gitClientHandler.isGitRepository()) {
            try {
           gitClientHandler.commitThenPushAndDisplayErrorMsg();
            } catch (IOException | GitAPIException e) {
                LOGGER.error("Failed to Push", e);
                dialogService.showErrorDialogAndWait(Localization.lang("Git Push Failed"),
                    Localization.lang("Failed to push changes: {0}", e.getMessage()));
            }
        } else {
            LOGGER.info("Not a git repository at path: {}", path);
            dialogService.showInformationDialogAndWait(Localization.lang("Git Push"), Localization.lang("This is not a Git repository"));
        }
    }
}
