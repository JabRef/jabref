package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.l10n.Localization;

import org.eclipse.jgit.api.errors.GitAPIException;

public class SaveGitDatabaseAction {
    final Path repositoryPath;
    final String automaticCommitMsg = "Automatic update via JabRef";

    private final DialogService dialogService;

    public SaveGitDatabaseAction(Path repositoryPath, DialogService dialogService) {
        this.repositoryPath = repositoryPath;
        this.dialogService = dialogService;
    }

    public boolean automaticUpdate() {
        try {
            GitHandler git = new GitHandler(repositoryPath);
            git.createCommitOnCurrentBranch(automaticCommitMsg, false);
            git.pushCommitsToRemoteRepository(this.dialogService);
        } catch (
                GitAPIException |
                IOException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), e);
            throw new RuntimeException(e);
        }

        return true;
    }
}
