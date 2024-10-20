package org.jabref.gui.mergeentries;

import java.util.List;
import java.util.function.Supplier;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for merging multiple entries with fetched data from identifiers like DOI or arXiv.
 */
public class MultiEntryMergeWithFetchedDataAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiEntryMergeWithFetchedDataAction.class);

    private final Supplier<LibraryTab> tabSupplier;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final UndoManager undoManager;

    public MultiEntryMergeWithFetchedDataAction(Supplier<LibraryTab> tabSupplier,
                                                GuiPreferences preferences,
                                                TaskExecutor taskExecutor,
                                                DialogService dialogService,
                                                UndoManager undoManager,
                                                StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.undoManager = undoManager;

        // Binding to only execute if a database is open
        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        LibraryTab libraryTab = tabSupplier.get();

        // Ensure that libraryTab is present
        if (libraryTab == null) {
            LOGGER.error("Action 'Multi Entry Merge' must be disabled when no database is open.");
            return;
        }

        List<BibEntry> selectedEntries = libraryTab.getDatabase().getEntries();

        // If no entries are selected, log and do not execute
        if (selectedEntries.isEmpty()) {
            dialogService.notify("No entries selected for merging.");
            return;
        }

        // Create an instance of FetchAndMergeEntry to perform the batch fetch and merge operation
        BibDatabaseContext bibDatabaseContext = libraryTab.getBibDatabaseContext();
        FetchAndMergeEntry fetchAndMergeEntry = new FetchAndMergeEntry(bibDatabaseContext, taskExecutor, preferences, dialogService, undoManager);

        fetchAndMergeEntry.fetchAndMergeBatch(selectedEntries);
    }
}
