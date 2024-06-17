package org.jabref.gui.search;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.shouldIndexLinkedFiles;

public class RebuildFulltextSearchIndexAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);

    private final StateManager stateManager;
    private final GetCurrentLibraryTab currentLibraryTab;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;

    private BibDatabaseContext databaseContext;

    private boolean shouldContinue = true;

    public RebuildFulltextSearchIndexAction(StateManager stateManager,
                                            GetCurrentLibraryTab currentLibraryTab,
                                            DialogService dialogService,
                                            PreferencesService preferences,
                                            TaskExecutor taskExecutor) {
        this.stateManager = stateManager;
        this.currentLibraryTab = currentLibraryTab;
        this.dialogService = dialogService;
        this.preferencesService = preferences;
        this.taskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(stateManager).and(shouldIndexLinkedFiles(preferences)));
    }

    @Override
    public void execute() {
        init();
        rebuildIndex();
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
        currentLibraryTab.get().getLuceneManager().rebuildIndex();
    }

    public interface GetCurrentLibraryTab {
        LibraryTab get();
    }
}
