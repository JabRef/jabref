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
            this.gitClientHandler.pullOnCurrentBranch();
        } catch (IOException e) {
            LOGGER.error("Failed to pull.", e);
        }
    }
}
