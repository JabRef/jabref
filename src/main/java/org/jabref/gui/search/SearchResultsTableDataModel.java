package org.jabref.gui.search;

import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import org.jabref.gui.StateManager;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableFieldValueFormatter;
import org.jabref.gui.maintable.NameDisplayPreferences;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class SearchResultsTableDataModel {

    private final SortedList<BibEntryTableViewModel> entriesSorted;
    private final ObjectProperty<MainTableFieldValueFormatter> fieldValueFormatter;
    private final StateManager stateManager;
    private final FilteredList<BibEntryTableViewModel> entriesFiltered;

    public SearchResultsTableDataModel(BibDatabaseContext bibDatabaseContext, PreferencesService preferencesService, StateManager stateManager) {
        NameDisplayPreferences nameDisplayPreferences = preferencesService.getNameDisplayPreferences();
        this.stateManager = stateManager;
        this.fieldValueFormatter = new SimpleObjectProperty<>(new MainTableFieldValueFormatter(nameDisplayPreferences, bibDatabaseContext));

        ObservableList<BibEntryTableViewModel> entriesViewModel = FXCollections.observableArrayList();
        populateEntriesViewModel(entriesViewModel);
        stateManager.getOpenDatabases().addListener((ListChangeListener<BibDatabaseContext>) change -> populateEntriesViewModel(entriesViewModel));

        entriesFiltered = new FilteredList<>(entriesViewModel);
        stateManager.searchResultSize(SearchType.GLOBAL_SEARCH).bind(Bindings.size(entriesFiltered));
        // We need to wrap the list since otherwise sorting in the table does not work
        entriesSorted = new SortedList<>(entriesFiltered);
    }

    private void populateEntriesViewModel(ObservableList<BibEntryTableViewModel> entriesViewModel) {
        entriesViewModel.clear();
        for (BibDatabaseContext context : stateManager.getOpenDatabases()) {
            ObservableList<BibEntry> entriesForDb = context.getDatabase().getEntries();
            List<BibEntryTableViewModel> viewModelForDb = EasyBind.mapBacked(entriesForDb, entry -> new BibEntryTableViewModel(entry, context, fieldValueFormatter));
            entriesViewModel.addAll(viewModelForDb);
        }
    }

    private boolean isMatchedBySearch(Optional<SearchQuery> query, BibEntryTableViewModel entry) {
        return query.map(matcher -> matcher.isMatch(entry.getEntry())).orElse(true);
    }

    public SortedList<BibEntryTableViewModel> getEntriesFilteredAndSorted() {
        return entriesSorted;
    }

    public void setBinding() {
        entriesFiltered.predicateProperty().bind(EasyBind.map(stateManager.activeSearchQuery(SearchType.GLOBAL_SEARCH), query -> entry -> isMatchedBySearch(query, entry)));
    }

    public void removeBinding() {
        entriesFiltered.predicateProperty().unbind();
    }
}
