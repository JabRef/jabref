package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.logic.git.GitHandler;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveGitDatabaseAction {
    static final Logger LOGGER = LoggerFactory.getLogger(GitHandler.class);
    final Path filePath;
    final String automaticCommitMsg = "Automatic update via JabRef";

    public SaveGitDatabaseAction(Path filePath) {
        this.filePath = filePath;
    }

    /**
    * Handle JabRef git integration action
    *
    * @return true of false whether the action was successful or not
    */
    public boolean automaticUpdate() {
        try {
            System.out.println(this.filePath.getParent());
            System.out.println(this.filePath.getFileName().toString());
            GitHandler git = new GitHandler(this.filePath.getParent());
            git.createCommitWithSingleFileOnCurrentBranch(this.filePath.getFileName().toString(), automaticCommitMsg, false);
            git.pushCommitsToRemoteRepository();
        } catch (
                GitAPIException |
                IOException e) {
                    System.out.println(e.getMessage());
            LOGGER.info("Failed to automatic git update");
        }

        return true;
    }
}
