
package org.jabref.gui.mergeentries;

import java.util.List;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.fetcher.MergingIdBasedFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiEntryMergeWithFetchedDataAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiEntryMergeWithFetchedDataAction.class);

    private final Supplier<LibraryTab> tabSupplier;
    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public MultiEntryMergeWithFetchedDataAction(Supplier<LibraryTab> tabSupplier,
                                                GuiPreferences preferences,
                                                DialogService dialogService,
                                                StateManager stateManager,
                                                TaskExecutor taskExecutor
    ) {
        this.tabSupplier = tabSupplier;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        LibraryTab libraryTab = tabSupplier.get();

        if (libraryTab == null) {
            LOGGER.error("Action 'Multi Entry Merge' must be disabled when no database is open.");
            return;
        }

        List<BibEntry> allEntries = libraryTab.getDatabase().getEntries();

        if (allEntries.isEmpty()) {
            dialogService.notify(Localization.lang("No entries exist."));
            return;
        }

        preferences.getImportFormatPreferences();
        new IsbnFetcher(preferences.getImportFormatPreferences());
        preferences.getImportFormatPreferences();

        MergingIdBasedFetcher mergingIdBasedFetcher = new MergingIdBasedFetcher(
                preferences,
                libraryTab.getUndoManager(),
                libraryTab.getBibDatabaseContext(),
                dialogService
        );

        BackgroundTask<List<String>> task = mergingIdBasedFetcher.fetchAndMergeBatch(allEntries);
        taskExecutor.execute(task);
    }
}
