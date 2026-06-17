package org.jabref.gui.entryeditor;

import java.util.SortedSet;

import javax.swing.undo.UndoManager;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.bibtex.TypedBibEntry;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fileformat.pdf.PdfMergeMetadataImporter;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.jspecify.annotations.Nullable;

public class EntryEditorViewModel extends AbstractViewModel {

    private final ObjectProperty<@Nullable BibEntry> currentlyEditedEntry = new SimpleObjectProperty<>();

    /// Display name of the current entry's type — the single source for the entry editor's type label.
    private final SimpleStringProperty typeLabelText = new SimpleStringProperty("");

    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final UndoManager undoManager;

    private @Nullable Subscription typeSubscription;

    public EntryEditorViewModel(StateManager stateManager,
                                GuiPreferences preferences,
                                TaskExecutor taskExecutor,
                                DialogService dialogService,
                                UndoManager undoManager) {
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.undoManager = undoManager;

        // [impl->req~entry-editor.keep-showing~1] — when selection becomes empty, keep the old entry showing
        stateManager.getSelectedEntries().addListener((InvalidationListener) _ -> {
            if (!stateManager.getSelectedEntries().isEmpty()) {
                currentlyEditedEntry.set(stateManager.getSelectedEntries().getFirst());
            }
        });

        // Keep the type label in sync with the current entry, its type, and the active database mode.
        EasyBind.subscribe(currentlyEditedEntry, this::rebindTypeLabel);
        EasyBind.subscribe(stateManager.activeDatabaseProperty(), _ -> updateTypeLabelText());
    }

    private void rebindTypeLabel(@Nullable BibEntry entry) {
        if (typeSubscription != null) {
            typeSubscription.unsubscribe();
            typeSubscription = null;
        }
        if (entry != null) {
            typeSubscription = EasyBind.subscribe(entry.typeProperty(), _ -> updateTypeLabelText());
        }
        updateTypeLabelText();
    }

    private void updateTypeLabelText() {
        BibEntry entry = currentlyEditedEntry.get();
        typeLabelText.set(entry == null
                ? ""
                : new TypedBibEntry(entry, activeDatabaseContext()).getTypeForDisplay());
    }

    private BibDatabaseContext activeDatabaseContext() {
        return stateManager.getActiveDatabase().orElseGet(BibDatabaseContext::new);
    }

    public ObjectProperty<@Nullable BibEntry> currentlyEditedEntryProperty() {
        return currentlyEditedEntry;
    }

    public @Nullable BibEntry getCurrentlyEditedEntry() {
        return currentlyEditedEntry.get();
    }

    public ReadOnlyStringProperty typeLabelTextProperty() {
        return typeLabelText;
    }

    public ObservableList<EntryEditorTabModel> getTabModels() {
        return preferences.getEntryEditorPreferences().getTabModels();
    }

    /// Entry-based fetchers available for the active library; used to populate the "fetch and merge" menu.
    public SortedSet<EntryBasedFetcher> getEntryBasedFetchers() {
        return WebFetchers.getEntryBasedFetchers(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getFilePreferences(),
                activeDatabaseContext());
    }

    public void fetchAndMerge(EntryBasedFetcher fetcher) {
        BibEntry entry = currentlyEditedEntry.get();
        if (entry == null) {
            return;
        }
        new FetchAndMergeEntry(activeDatabaseContext(), taskExecutor, preferences, dialogService, undoManager, stateManager)
                .fetchAndMerge(entry, fetcher);
    }

    /// Fetches and merges bibliographic data from the linked PDF's embedded metadata.
    /// The Grobid opt-in (a UI concern) is handled by the caller before invoking this.
    public void fetchAndMergeFromPdfMetadata() {
        PdfMergeMetadataImporter.EntryBasedFetcherWrapper fetcher =
                new PdfMergeMetadataImporter.EntryBasedFetcherWrapper(
                        preferences.getImportFormatPreferences(),
                        preferences.getFilePreferences(),
                        activeDatabaseContext());
        fetchAndMerge(fetcher);
    }
}
