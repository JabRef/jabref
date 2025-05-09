package org.jabref.gui.search;

import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class RebuildFulltextSearchIndexAction extends SimpleCommand {

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final Supplier<LibraryTab> tabSupplier;
    private boolean shouldContinue = true;

    public RebuildFulltextSearchIndexAction(StateManager stateManager,
                                            Supplier<LibraryTab> tabSupplier,
                                            DialogService dialogService,
                                            CliPreferences preferences) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.tabSupplier = tabSupplier;
        this.executable.bind(needsDatabase(stateManager).and(preferences.getFilePreferences().fulltextIndexLinkedFilesProperty()));
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
        tabSupplier.get().getIndexManager().rebuildFullTextIndex();
    }
}
