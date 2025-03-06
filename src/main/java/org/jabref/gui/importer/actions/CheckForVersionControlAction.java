package org.jabref.gui.importer.actions;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When opening a file that is under Git version control, a pull operation is
 * attempted.
 */
public class CheckForVersionControlAction implements GUIPostOpenAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckForVersionControlAction.class);
    private GitHandler gitHandler;

    @Override
    public boolean isActionNecessary(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        try {
            if (!parserResult.getDatabaseContext().getDatabasePath().isPresent()) {
                return false;
            }

            Path databasePath = parserResult.getDatabaseContext().getDatabasePath().get();

            // First do a quick check if .git directory exists
            Path gitDir = databasePath.getParent().resolve(".git");
            boolean gitDirExists = Files.exists(gitDir) && Files.isDirectory(gitDir);

            if (gitDirExists) {
                // Preemptively mark as under version control
                parserResult.getDatabaseContext().setUnderVersionControl(true);
                return true;
            }

            // Fallback to more thorough check
            try {
                this.gitHandler = new GitHandler(databasePath);
                boolean isGitRepo = this.gitHandler.isGitRepository();
                if (isGitRepo) {
                    parserResult.getDatabaseContext().setUnderVersionControl(true);
                }
                return isGitRepo;
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        if (!parserResult.getDatabaseContext().getDatabasePath().isPresent()) {
            return;
        }

        Path databasePath = parserResult.getDatabaseContext().getDatabasePath().get();

        try {
            GitHandler gitHandler = new GitHandler(databasePath);

            // Check if file is in a git repository
            if (gitHandler.isGitRepository()) {
                parserResult.getDatabaseContext().setUnderVersionControl(true);

                // Mark that the database is under version control
                if (parserResult.getDatabaseContext().getDatabase().hasEntries() && gitHandler.isGitRepository()) {
                    try {
                        boolean pullSuccessful = gitHandler.pullOnCurrentBranch();

                        if (pullSuccessful) {
                            dialogService.showInformationDialogAndWait(
                                    "Git Repository",
                                    "Successfully synchronized with Git repository");
                        }
                    } catch (Exception e) {
                        dialogService.showErrorDialogAndWait(
                                "Git Repository Error",
                                "Could not pull changes from Git repository: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            dialogService.showErrorDialogAndWait(
                    "Git Repository Error",
                    "Error checking Git repository status: " + e.getMessage());
        }
    }
}
