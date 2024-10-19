package org.jabref.gui.mergeentries;

import java.util.List;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

/**
 * Action for merging multiple entries with fetched data from identifiers like DOI or arXiv.
 */
public class MultiEntryMergeWithFetchedDataAction extends SimpleCommand {

    private final LibraryTab libraryTab;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;
    private final BibDatabaseContext bibDatabaseContext;
    private final DialogService dialogService;
    private final UndoManager undoManager;

    public MultiEntryMergeWithFetchedDataAction(LibraryTab libraryTab,
                                                GuiPreferences preferences,
                                                TaskExecutor taskExecutor,
                                                BibDatabaseContext bibDatabaseContext,
                                                DialogService dialogService,
                                                UndoManager undoManager) {
        this.libraryTab = libraryTab;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.bibDatabaseContext = bibDatabaseContext;
        this.dialogService = dialogService;
        this.undoManager = undoManager;
    }

    @Override
    public void execute() {
        List<BibEntry> selectedEntries = libraryTab.getSelectedEntries();

        // Create an instance of FetchAndMergeEntry
        FetchAndMergeEntry fetchAndMergeEntry = new FetchAndMergeEntry(bibDatabaseContext, taskExecutor, preferences, dialogService, undoManager);

        // Perform the batch fetch and merge operation
        fetchAndMergeEntry.fetchAndMergeBatch(selectedEntries);
    }
}
