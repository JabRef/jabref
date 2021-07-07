package org.jabref.gui.search;

import java.io.IOException;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.logic.pdf.search.indexing.PdfIndexer;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.FilePreferences;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class RebuildFulltextSearchIndexAction extends SimpleCommand {

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final IndexingTaskManager indexingTaskManager;
    private final FilePreferences filePreferences;

    private BibDatabaseContext databaseContext;

    private boolean shouldContinue = true;

    public RebuildFulltextSearchIndexAction(StateManager stateManager, DialogService dialogService, IndexingTaskManager indexingTaskManager, FilePreferences filePreferences) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.indexingTaskManager = indexingTaskManager;
        this.filePreferences = filePreferences;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        init();
        BackgroundTask.wrap(this::rebuildIndex)
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    public void init() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        databaseContext = stateManager.getActiveDatabase().get();
        boolean confirm = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Rebuild fulltext search index"),
                Localization.lang("Rebuild fulltext search index for current library?"));
        if (!confirm) {
            shouldContinue = false;
            return;
        }
        dialogService.notify(Localization.lang("Rebuilding fulltext search index..."));
    }

    private void rebuildIndex() {
        if (!shouldContinue || stateManager.getActiveDatabase().isEmpty()) {
            return;
        }
        try {
            indexingTaskManager.createIndex(PdfIndexer.of(databaseContext, filePreferences), databaseContext.getDatabase(), databaseContext);
        } catch (IOException e) {
            dialogService.notify(Localization.lang("Failed to access fulltext search index"));
        }
    }
}
