package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.documentviewer.DocumentViewerView;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.l10n.Localization;
import com.airhacks.afterburner.injection.Injector;
import org.eclipse.jgit.api.errors.GitAPIException;

public class SaveGitDatabaseAction {
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
            dialogService.showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), e);
            throw new RuntimeException(e);
        }

        return true;
    }
}
