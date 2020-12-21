package org.jabref.gui.referencemetadata;

import java.util.LinkedList;
import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.collections.ObservableList;

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
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

/**
 * This class allows fetching the reference metadata (e.g. citation counts) from the web for the currently selected
 * entries in a library.
 */
public class ReferenceMetadataFetcherAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceMetadataFetcherAction.class);

    private static boolean USE_REFERENCE_METADATA_FETCHER_GOOGLE_SCHOLAR = true;
    private static boolean USE_REFERENCE_METADATA_FETCHER_SEMANTIC_SCHOLAR = true;
    private static boolean USE_REFERENCE_METADATA_FETCHER_OPEN_CITATIONS = true;

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final StateManager stateManager;
    private UndoManager undoManager;
    private TaskExecutor taskExecutor;

    public ReferenceMetadataFetcherAction(DialogService dialogService, PreferencesService preferences, StateManager stateManager, UndoManager undoManager, TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
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

        ExtendedTask<List<BibEntry>> fetchReferenceMetadataTask = new ExtendedTask<List<BibEntry>>() {

            @Override
            protected List<BibEntry> call() {

                boolean processCancelled = false;

                ObservableList<BibEntry> entriesWithIncompleteMetadata = entries;

                // run prioritized metadata fetcher pipeline

                if (!processCancelled && USE_REFERENCE_METADATA_FETCHER_GOOGLE_SCHOLAR && entriesWithIncompleteMetadata.size() > 0) {
                    LOGGER.info("running " + ReferenceMetadataFetcherGoogleScholar.class.getSimpleName() + "...");
                    ReferenceMetadataFetcherGoogleScholar referenceMetadataFetcherGoogleScholar = new ReferenceMetadataFetcherGoogleScholar();
                    processCancelled = referenceMetadataFetcherGoogleScholar.fetchFor(database, entriesWithIncompleteMetadata, dialogService, this);
                    entriesWithIncompleteMetadata = referenceMetadataFetcherGoogleScholar.getEntriesWithIncompleteMetadata();
                }

                if (!processCancelled && USE_REFERENCE_METADATA_FETCHER_SEMANTIC_SCHOLAR && entriesWithIncompleteMetadata.size() > 0) {
                    LOGGER.info("running " + ReferenceMetadataFetcherSemanticScholar.class.getSimpleName() + "...");
                    ReferenceMetadataFetcherSemanticScholar referenceMetadataFetcherSemanticScholar = new ReferenceMetadataFetcherSemanticScholar();
                    processCancelled = referenceMetadataFetcherSemanticScholar.fetchFor(database, entriesWithIncompleteMetadata, dialogService, this);
                    entriesWithIncompleteMetadata = referenceMetadataFetcherSemanticScholar.getEntriesWithIncompleteMetadata();
                }

                if (!processCancelled && USE_REFERENCE_METADATA_FETCHER_OPEN_CITATIONS && entriesWithIncompleteMetadata.size() > 0) {
                    LOGGER.info("running " + ReferenceMetadataFetcherOpenCitations.class.getSimpleName() + "...");
                    ReferenceMetadataFetcherOpenCitations referenceMetadataFetcherOpenCitations = new ReferenceMetadataFetcherOpenCitations();
                    processCancelled = referenceMetadataFetcherOpenCitations.fetchFor(database, entriesWithIncompleteMetadata, dialogService, this);
                    entriesWithIncompleteMetadata = referenceMetadataFetcherOpenCitations.getEntriesWithIncompleteMetadata();
                }

                if (!processCancelled) {
                    return entries;
                } else {
                    return new LinkedList<BibEntry>();
                }
            }

            @Override
            protected void succeeded() {
                if (!getValue().isEmpty()) {
                    // reserved for future use
                    // if (nc.hasEdits()) {
                    //    nc.end();
                    //    undoManager.addEdit(nc);
                    // }
                    dialogService.notify(Localization.lang("Finished fetching reference metadata."));
                } else {
                    dialogService.notify(Localization.lang("Cancelled fetching reference metadata."));
                }
            }
        };

        dialogService.showProgressDialog(
                Localization.lang("Fetching reference metadata online"),
                Localization.lang("Querying reference metadata") + ":",
                fetchReferenceMetadataTask);
        taskExecutor.execute(fetchReferenceMetadataTask);
    }
}
