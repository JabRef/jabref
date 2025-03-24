package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.git.GitClientHandler;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action checks whether this BIB file is contained in a Git repository. If so,
 * then the file is tagged as "versioned" in BibDatabaseContext and a git pull is
 * attempted.
 */
public class CheckForVersionControlAction implements GUIPostOpenAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckForVersionControlAction.class);
    private GitClientHandler gitClientHandler;

    @Override
    public boolean isActionNecessary(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        Optional<Path> path = parserResult.getDatabaseContext().getDatabasePath();
        if (path.isEmpty()) {
            return false;
        }
        this.gitClientHandler = new GitClientHandler(path.get(),
                dialogService,
                preferences);
        return gitClientHandler.isGitRepository();
    }

    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, CliPreferences preferencesService) {
        parserResult.getDatabaseContext().setVersioned(true);

        try {
            boolean pullSuccessful = this.gitClientHandler.pullOnCurrentBranch();
            if (pullSuccessful) {
                dialogService.notify("Successfully Pulled changes from remote repository");
            } else {
                dialogService.showErrorDialogAndWait("Git Pull Failed", 
                        "Failed to pull changes from remote repository.\n\n" +
                        "MOST LIKELY CAUSE: Missing Git credentials.\n" +
                        "Please set your credentials by either:\n" +
                        "1. Setting GIT_EMAIL and GIT_PW environment variables, or\n" +
                        "2. Configuring them in JabRef Preferences\n\n" +
                        "Other possible causes:\n" +
                        "- Network connectivity issues\n" +
                        "- Merge conflicts\n" +
                        "- Remote repository inaccessible");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to Pull", e);
            dialogService.showErrorDialogAndWait("Git Pull Failed", "Failed to pull changes: " + e.getMessage());
        }
    }
}
