package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.logic.git.GitHandler;

import com.airhacks.afterburner.injection.Injector;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveGitDatabaseAction {
    static final Logger LOGGER = LoggerFactory.getLogger(GitHandler.class);
    final Path filePath;
    final String automaticCommitMsg = "Automatic update via JabRef";

    private final DialogService dialogService;

    public SaveGitDatabaseAction(Path filePath) {
        this.filePath = filePath;
        this.dialogService = Injector.instantiateModelOrService(DialogService.class);
    }

    /**
    * Handle JabRef git integration action
    *
    * @return true of false whether the action was successful or not
    */
    public boolean automaticUpdate() {
        try {
            GitHandler git = new GitHandler(filePath.getParent());
            git.createCommitWithSingleFileOnCurrentBranch(automaticCommitMsg, filePath.getFileName().toString(), false);
            git.pushCommitsToRemoteRepository();
        } catch (
                GitAPIException |
                IOException e) {
            LOGGER.info("Failed to automatic update");
        }

        return true;
    }
}
