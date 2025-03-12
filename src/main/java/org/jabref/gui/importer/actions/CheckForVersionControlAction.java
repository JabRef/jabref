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
 * When opening a file, this action checks if it is under Git version control
 * and marks the database context accordingly if it is.
 */
public class CheckForVersionControlAction implements GUIPostOpenAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckForVersionControlAction.class);
    private GitHandler gitHandler;

    @Override
    public boolean isActionNecessary(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        try {
            if (parserResult.getDatabaseContext().getDatabasePath().isEmpty()) {
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
            } catch (IOException e) {
                LOGGER.debug("IO error checking Git repository", e);
                return false;
            }
        } catch (NullPointerException | SecurityException e) {
            LOGGER.debug("Error accessing repository path", e);
            return false;
        }
    }

    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        if (parserResult.getDatabaseContext().getDatabasePath().isEmpty()) {
            return;
        }

        Path databasePath = parserResult.getDatabaseContext().getDatabasePath().get();

        try {
            GitHandler gitHandler = new GitHandler(databasePath);

            // Check if file is in a git repository
            if (gitHandler.isGitRepository()) {
                parserResult.getDatabaseContext().setUnderVersionControl(true);
            }
        } catch (IOException e) {
            LOGGER.error("IO error when checking Git repository status", e);
            dialogService.showErrorDialogAndWait(
                    "Git Repository Error",
                    "Error accessing Git repository: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid path when checking Git repository status", e);
            dialogService.showErrorDialogAndWait(
                    "Git Repository Error",
                    "Invalid file path for Git repository: " + e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected error when checking Git repository status", e);
            dialogService.showErrorDialogAndWait(
                    "Git Repository Error",
                    "Error checking Git repository status: " + e.getMessage());
        }
    }
}
