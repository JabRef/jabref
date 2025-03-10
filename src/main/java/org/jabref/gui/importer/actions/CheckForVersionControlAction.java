package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;

/**
 * This action checks whether this BIB file is contained in a Git repository. If so,
 * then the file is tagged as "versioned" in BibDatabaseContext and a git pull is
 * attempted.
 */
public class CheckForVersionControlAction implements GUIPostOpenAction {
    private GitHandler gitHandler;

    @Override
    public boolean isActionNecessary(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        Optional<Path> path = parserResult.getDatabaseContext().getDatabasePath();
        if (path.isEmpty()) {
            return false;
        } else {
            this.gitHandler = new GitHandler(path.get());
            return gitHandler.isGitRepository();
        }
    }

    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, CliPreferences preferencesService) {
        // TODO: Tag as versioned
        // TODO: If the preference exists, only pull if the user has this preference on

        try {
            this.gitHandler.pullOnCurrentBranch();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
