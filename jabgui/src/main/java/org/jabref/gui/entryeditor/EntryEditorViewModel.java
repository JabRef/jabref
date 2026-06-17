package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.importer.GrobidUseDialogHelper;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.bibtex.TypedBibEntry;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fileformat.pdf.PdfMergeMetadataImporter;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.jspecify.annotations.Nullable;

public class EntryEditorViewModel extends AbstractViewModel {

    private final ObjectProperty<@Nullable BibEntry> currentlyEditedEntry = new SimpleObjectProperty<>();

    /// Display name of the current entry's type — the single source for the entry editor's type label.
    private final SimpleStringProperty typeLabelText = new SimpleStringProperty("");

    /// The current entry's type, updated by the single {@link #typeSubscription}. The editor observes this to
    /// rebuild its toolbar, so there is no second listener on the entry's {@code typeProperty}.
    private final ObjectProperty<@Nullable EntryType> currentEntryType = new SimpleObjectProperty<>();

    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final UndoManager undoManager;
    private final EntryEditorTabFactory tabFactory;

    /// Every tab that can possibly be shown for the current entry, in display order.
    private final List<EntryEditorTab> allPossibleTabs = new ArrayList<>();
    /// One subscription per tab, observing its {@link EntryEditorTab#shouldShow()}.
    private final List<Subscription> shouldShowSubscriptions = new ArrayList<>();
    /// The subset of {@link #allPossibleTabs} currently shown — mutated incrementally so a bound TabPane
    /// replays adds/removes one by one (a full replace causes an ugly shift-in animation).
    private final ObservableList<Tab> visibleTabs = FXCollections.observableArrayList();
    /// Read-only view handed to the editor for {@code Bindings.bindContent}. Held in a field on purpose: the
    /// unmodifiable wrapper keeps only a weak listener on {@link #visibleTabs}, so it must stay strongly
    /// referenced or a GC silently kills the content binding (the TabPane would stop updating).
    private final ObservableList<Tab> unmodifiableVisibleTabs = FXCollections.unmodifiableObservableList(visibleTabs);
    private @Nullable SourceTab sourceTab;

    /// Single subscription to the current entry's {@code typeProperty}; feeds both the type label and
    /// {@link #currentEntryType}.
    private @Nullable Subscription typeSubscription;

    public EntryEditorViewModel(StateManager stateManager,
                                GuiPreferences preferences,
                                TaskExecutor taskExecutor,
                                DialogService dialogService,
                                UndoManager undoManager,
                                EntryEditorTabFactory tabFactory) {
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.undoManager = undoManager;
        this.tabFactory = tabFactory;

        // [impl->req~entry-editor.keep-showing~1] — when selection becomes empty, keep the old entry showing
        stateManager.getSelectedEntries().addListener((InvalidationListener) _ -> {
            if (!stateManager.getSelectedEntries().isEmpty()) {
                currentlyEditedEntry.set(stateManager.getSelectedEntries().getFirst());
            }
        });

        // Keep the type label and the exposed entry type in sync with the current entry, its type, and the
        // active database mode, through a single subscription to the entry's typeProperty.
        EasyBind.subscribe(currentlyEditedEntry, this::rebindToEntryType);
        EasyBind.subscribe(stateManager.activeDatabaseProperty(), _ -> updateTypeLabelText());

        // Rebuild the tab set whenever the configured tab models change (tabs added, removed or reordered in
        // the preferences). This observes the single source of truth directly, replacing the former
        // AdaptVisibleTabs callback that other view models used to poke the editor.
        preferences.getEntryEditorPreferences().getTabModels()
                   .addListener((InvalidationListener) _ -> rebuildTabs());
    }

    /// (Re)subscribes the single listener on the current entry's type, driving both the type label and the
    /// {@link #currentEntryType} property that the editor observes.
    private void rebindToEntryType(@Nullable BibEntry entry) {
        if (typeSubscription != null) {
            typeSubscription.unsubscribe();
            typeSubscription = null;
        }
        if (entry != null) {
            typeSubscription = EasyBind.subscribe(entry.typeProperty(), type -> {
                currentEntryType.set(type);
                updateTypeLabelText();
            });
        } else {
            currentEntryType.set(null);
            updateTypeLabelText();
        }
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

    /// The current entry's type, exposed so the editor can rebuild its toolbar when the type changes. Backed by
    /// the single {@link #typeSubscription} shared with the type label — no duplicate listener on the entry.
    public ObservableValue<@Nullable EntryType> currentEntryTypeProperty() {
        return currentEntryType;
    }

    /// Live, ordered list of the tabs that should currently be shown for the edited entry. The
    /// {@link EntryEditor} binds its {@code TabPane} to this; it is mutated incrementally to avoid a full replace.
    public ObservableList<Tab> visibleTabs() {
        return unmodifiableVisibleTabs;
    }

    /// All tabs that can possibly be shown for the current entry (regardless of visibility), in display order.
    public List<EntryEditorTab> getAllPossibleTabs() {
        return List.copyOf(allPossibleTabs);
    }

    /// The source tab, if present, so the View can select it by default.
    public Optional<SourceTab> sourceTab() {
        return Optional.ofNullable(sourceTab);
    }

    /// Recreates every possible tab from the factory, binds each to the current entry, subscribes to each tab's
    /// {@link EntryEditorTab#shouldShow()}, and re-renders the visible set. Called by the View when the active
    /// library tab changes, and internally when the configured tab models change.
    public void rebuildTabs() {
        shouldShowSubscriptions.forEach(Subscription::unsubscribe);
        shouldShowSubscriptions.clear();

        allPossibleTabs.clear();
        allPossibleTabs.addAll(tabFactory.createTabs());
        sourceTab = allPossibleTabs.stream()
                                   .filter(SourceTab.class::isInstance)
                                   .map(SourceTab.class::cast)
                                   .findFirst()
                                   .orElse(null);

        // Each tab observes the currently edited entry directly, so its content-driven visibility reacts to
        // entry and entry-type changes without the editor pushing the entry into every tab by hand.
        allPossibleTabs.forEach(tab -> tab.currentEntryProperty().bind(currentlyEditedEntry));

        allPossibleTabs.forEach(tab ->
                shouldShowSubscriptions.add(EasyBind.subscribe(tab.shouldShow(), _ -> refreshVisibleTabs())));
        refreshVisibleTabs();
    }

    /// Tears down the tab state when no entry is being edited (e.g. the last library was closed).
    public void clearTabs() {
        shouldShowSubscriptions.forEach(Subscription::unsubscribe);
        shouldShowSubscriptions.clear();
        allPossibleTabs.clear();
        visibleTabs.clear();
    }

    /// Diffs {@link #allPossibleTabs} against {@link #visibleTabs} and adds/removes to match the tabs whose
    /// {@link EntryEditorTab#shouldShow()} is {@code true}, preserving order without a full replace.
    private void refreshVisibleTabs() {
        if (currentlyEditedEntry.get() == null) {
            visibleTabs.clear();
            return;
        }

        visibleTabs.removeAll(allPossibleTabs.stream().filter(tab -> !tab.shouldShow().getValue()).toList());

        List<Tab> wanted = allPossibleTabs.stream().filter(tab -> tab.shouldShow().getValue()).collect(Collectors.toList());
        for (int i = 0; i < wanted.size(); i++) {
            Tab toBeAdded = wanted.get(i);
            Tab shown = i < visibleTabs.size() ? visibleTabs.get(i) : null;
            if (!toBeAdded.equals(shown)) {
                visibleTabs.add(i, toBeAdded);
            }
        }
    }

    /// Entry-based fetchers available for the active library; used to populate the "fetch and merge" menu.
    public SortedSet<EntryBasedFetcher> getEntryBasedFetchers() {
        return WebFetchers.getEntryBasedFetchers(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getFilePreferences(),
                activeDatabaseContext());
    }

    /// Fetches and merges bibliographic data for the current entry from the given fetcher. The PDF-metadata
    /// fetcher first resolves the Grobid opt-in with the user — that policy lives here, not in the editor.
    public void fetchAndMerge(EntryBasedFetcher fetcher) {
        BibEntry entry = currentlyEditedEntry.get();
        if (entry == null) {
            return;
        }
        if (fetcher instanceof PdfMergeMetadataImporter.EntryBasedFetcherWrapper) {
            GrobidUseDialogHelper.showAndWaitIfUserIsUndecided(dialogService, preferences.getGrobidPreferences());
        }
        new FetchAndMergeEntry(activeDatabaseContext(), taskExecutor, preferences, dialogService, undoManager, stateManager)
                .fetchAndMerge(entry, fetcher);
    }
}
