package org.jabref.gui.search;

import java.io.IOException;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.indexing.PdfIndexer;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class RebuildFulltextSearchIndexAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);

    private final StateManager stateManager;
    private final GetCurrentLibraryTab currentLibraryTab;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;

    private BibDatabaseContext databaseContext;

    private boolean shouldContinue = true;

    public RebuildFulltextSearchIndexAction(StateManager stateManager, GetCurrentLibraryTab currentLibraryTab, DialogService dialogService, FilePreferences filePreferences) {
        this.stateManager = stateManager;
        this.currentLibraryTab = currentLibraryTab;
        this.dialogService = dialogService;
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
            currentLibraryTab.get().getIndexingTaskManager().createIndex(PdfIndexer.of(databaseContext, filePreferences));
            currentLibraryTab.get().getIndexingTaskManager().addToIndex(PdfIndexer.of(databaseContext, filePreferences), databaseContext);
        } catch (IOException e) {
            dialogService.notify(Localization.lang("Failed to access fulltext search index"));
            LOGGER.error("Failed to access fulltext search index", e);
        }
    }

    public interface GetCurrentLibraryTab {
        LibraryTab get();
    }
}
