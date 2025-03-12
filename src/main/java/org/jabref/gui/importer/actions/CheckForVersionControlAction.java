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

    /**
     * Checks if the file is in a Git repository and marks the database context accordingly.
     *
     * @param parserResult  The parser result containing the database context
     * @param dialogService The dialog service to show error messages, can be null if no error dialogs should be shown
     * @return true if the file is in a Git repository, false otherwise
     */
    private boolean checkAndMarkGitRepository(ParserResult parserResult, DialogService dialogService) {
        if (parserResult.getDatabaseContext().getDatabasePath().isEmpty()) {
            return false;
        }

        Path databasePath = parserResult.getDatabaseContext().getDatabasePath().get();

        try {
            // First do a quick check if .git directory exists
            Path gitDir = databasePath.getParent().resolve(".git");
            boolean gitDirExists = Files.exists(gitDir) && Files.isDirectory(gitDir);

            if (gitDirExists) {
                // Preemptively mark as under version control
                parserResult.getDatabaseContext().setUnderVersionControl(true);
                return true;
            }

            // Fallback to more thorough check
            GitHandler gitHandler = new GitHandler(databasePath);
            boolean isGitRepo = gitHandler.isGitRepository();
            if (isGitRepo) {
                parserResult.getDatabaseContext().setUnderVersionControl(true);
            }
            return isGitRepo;
        } catch (NullPointerException e) {
            // Specific handling for null pointer
            LOGGER.debug("Null pointer encountered when checking Git repository", e);
            return false;
        } catch (SecurityException e) {
            // Specific handling for security issues
            LOGGER.debug("Security error when accessing repository path", e);
            return false;
        } catch (IllegalArgumentException e) {
            // Specific handling for invalid arguments
            LOGGER.error("Invalid path when checking Git repository status", e);
            if (dialogService != null) {
                dialogService.showErrorDialogAndWait(
                        "Git Repository Error",
                        "Invalid file path for Git repository: " + e.getMessage());
            }
            return false;
        } catch (RuntimeException e) {
            // Catch other runtime exceptions
            LOGGER.error("Unexpected error when checking Git repository status", e);
            if (dialogService != null) {
                dialogService.showErrorDialogAndWait(
                        "Git Repository Error",
                        "Error checking Git repository status: " + e.getMessage());
            }
            return false;
        }
    }

    @Override
    public boolean isActionNecessary(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        // We don't need to show error dialogs here, just return the result
        return checkAndMarkGitRepository(parserResult, null);
    }

    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        // Here we want to show error dialogs if something goes wrong
        checkAndMarkGitRepository(parserResult, dialogService);
    }
}
