package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.model.MergeResult;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * - Check if Git is enabled
 * - Verify activeDatabase is not null
 * - Call GitPullViewModel.pull()
 */
public class GitPullAction extends SimpleCommand {

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences guiPreferences;
    private final UndoManager undoManager;

    public GitPullAction(DialogService dialogService,
                         StateManager stateManager,
                         GuiPreferences guiPreferences,
                         UndoManager undoManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.guiPreferences = guiPreferences;
        this.undoManager = undoManager;
    }

    @Override
    public void execute() {
        // TODO: reconsider error handling
        if (stateManager.getActiveDatabase().isEmpty()) {
            dialogService.showErrorDialogAndWait("No database open", "Please open a database before pulling.");
            return;
        }

        BibDatabaseContext database = stateManager.getActiveDatabase().get();
        if (database.getDatabasePath().isEmpty()) {
            dialogService.showErrorDialogAndWait("No .bib file path", "Cannot pull from Git: No file is associated with this database.");
            return;
        }

        Path bibFilePath = database.getDatabasePath().get();
        try {
            GitPullViewModel viewModel = new GitPullViewModel(
                    guiPreferences.getImportFormatPreferences(),
                    new GitConflictResolverViaDialog(dialogService, guiPreferences),
                    dialogService
            );
            MergeResult result = viewModel.pull(bibFilePath);

            if (result.isSuccessful()) {
                dialogService.showInformationDialogAndWait("Git Pull", "Successfully merged and updated.");
            } else {
                dialogService.showWarningDialogAndWait("Git Pull", "Merge completed with conflicts.");
            }
        } catch (JabRefException e) {
            dialogService.showErrorDialogAndWait("Git Pull Failed", e);
            // TODO: error handling
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
