package org.jabref.gui.referencemetadata;

import java.util.LinkedList;
import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

/**
 * This class allows fetching the reference metadata (e.g. citation counts) from the web for the currently selected entries in a library.
 */
public class ReferenceMetadataFetcher extends SimpleCommand {

    private static boolean USE_REFERENCE_METADATA_FETCHER_GOOGLE_SCHOLAR = true;

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final StateManager stateManager;
    private UndoManager undoManager;
    private TaskExecutor taskExecutor;

    public ReferenceMetadataFetcher(JabRefFrame frame, JabRefPreferences preferences, StateManager stateManager, UndoManager undoManager, TaskExecutor taskExecutor) {
        this.dialogService = frame.getDialogService();
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.taskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(this.stateManager).and(needsEntriesSelected(stateManager)));
        this.statusMessage.bind(BindingsHelper.ifThenElse(executable, Localization.lang("This operation fetches " +
                "reference metadata for the currently selected entries online."), Localization.lang("This " +
                "operation requires one or more entries to be selected.")));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        ObservableList<BibEntry> entries = stateManager.getSelectedEntries();

        final NamedCompound nc = new NamedCompound(Localization.lang("Fetch reference metadata"));

        Task<List<BibEntry>> fetchReferenceMetadataTask = new Task<List<BibEntry>>() {

            @Override
            protected List<BibEntry> call() {

                boolean success = false;

                if (USE_REFERENCE_METADATA_FETCHER_GOOGLE_SCHOLAR) {
                    ReferenceMetadataFetcherGoogleScholar referenceMetadataFetcherGoogleScholar = new ReferenceMetadataFetcherGoogleScholar();
                    success = referenceMetadataFetcherGoogleScholar.fetchFor(database, entries, dialogService);
                }

                if (success) {
                    return entries;
                }
                else {
                    return new LinkedList<BibEntry>();
                }
            }

            @Override
            protected void succeeded() {
                if (!getValue().isEmpty()) {
                    // reserved for future use
                    //if (nc.hasEdits()) {
                    //    nc.end();
                    //    undoManager.addEdit(nc);
                    //}
                    dialogService.notify(Localization.lang("Finished fetching reference metadata."));
                } else {
                    dialogService.notify(Localization.lang("Cancelled fetching reference metadata."));
                }
            }
        };

        dialogService.showProgressDialogAndWait(
                Localization.lang("Fetching reference metadata online"),
                Localization.lang("Querying reference metadata..."),
                fetchReferenceMetadataTask);
        taskExecutor.execute(fetchReferenceMetadataTask);
    }
}
