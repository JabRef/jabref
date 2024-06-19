package org.jabref.gui.search;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.indexing.LuceneManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.shouldIndexLinkedFiles;

public class RebuildFulltextSearchIndexAction extends SimpleCommand {

    private final StateManager stateManager;
    private final DialogService dialogService;
    private BibDatabaseContext databaseContext;
    private boolean shouldContinue = true;

    public RebuildFulltextSearchIndexAction(StateManager stateManager,
                                            DialogService dialogService,
                                            PreferencesService preferences) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
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
        LuceneManager.get(databaseContext).rebuildIndex();
    }
}
