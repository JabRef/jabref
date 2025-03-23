package org.jabref.gui.shared;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.git.GitClientHandler;
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
                preferences.getGitPreferences());
        if (gitClientHandler.isGitRepository()) {
            try {
               boolean commitCreated = gitClientHandler.createCommitOnCurrentBranch("Automatic update via JabRef", false);
               if (commitCreated) {
                    boolean successPush = gitClientHandler.pushCommitsToRemoteRepository();
                    if (successPush) {
                        dialogService.notify("Successfully Pushed changes to remote repository");
                    } else {
                        dialogService.showErrorDialogAndWait("Git Push Failed", 
                            "Failed to push changes to remote repository.\n\n" +
                            "MOST LIKELY CAUSE: Missing Git credentials.\n" +
                            "Please set your credentials by either:\n" +
                            "1. Setting GIT_EMAIL and GIT_PW environment variables, or\n" +
                            "2. Configuring them in JabRef Preferences\n\n" +
                            "Other possible causes:\n" +
                            "- Network connectivity issues\n" +
                            "- Remote repository rejecting the push");
                    }
                } else {
                    dialogService.showInformationDialogAndWait("Git Push", "No changes to push");
                }
            } catch (IOException | GitAPIException e) {
                LOGGER.error("Failed to Push", e);
                dialogService.showErrorDialogAndWait("Git Push Failed", "Failed to push changes: " + e.getMessage());
            }
        } else {
            LOGGER.info("Not a git repository at path: {}", path);
            dialogService.showInformationDialogAndWait("Git Push", "This is not a Git repository");
        }
    }
}
