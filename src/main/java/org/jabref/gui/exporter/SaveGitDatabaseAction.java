package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.logic.git.GitHandler;
import org.jabref.preferences.PreferencesService;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveGitDatabaseAction {
    static final Logger LOGGER = LoggerFactory.getLogger(GitHandler.class);
    final Path filePath;
    final String automaticCommitMsg = "Automatic update via JabRef";
    private final PreferencesService preferences;

    public SaveGitDatabaseAction(Path filePath, PreferencesService preferences) {
        this.filePath = filePath;
        this.preferences = preferences;
    }

    /**
    * Handle JabRef git integration action
    *
    * @return true of false whether the action was successful or not
    */
    public boolean automaticUpdate() {
        try {
            GitHandler git = new GitHandler(this.filePath.getParent());
            git.setGitPreferences(preferences.getGitPreferences());
            git.createCommitWithSingleFileOnCurrentBranch(this.filePath.getFileName().toString(), automaticCommitMsg, false);
            git.pushCommitsToRemoteRepository();
        } catch (GitAPIException | IOException e) {
            LOGGER.info("Failed to automatic git update");
        }

        return true;
    }
}
