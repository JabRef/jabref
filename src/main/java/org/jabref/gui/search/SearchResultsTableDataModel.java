package org.jabref.gui.search;

import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;

import org.jabref.gui.StateManager;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableFieldValueFormatter;
import org.jabref.gui.maintable.NameDisplayPreferences;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.CustomFilteredList;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class SearchResultsTableDataModel {

    private final ObservableList<BibEntryTableViewModel> entriesViewModel = FXCollections.observableArrayList();
    private final SortedList<BibEntryTableViewModel> entriesSorted;
    private final ObjectProperty<MainTableFieldValueFormatter> fieldValueFormatter;
    private final StateManager stateManager;
    private final CustomFilteredList<BibEntryTableViewModel> entriesFiltered;
    private final TaskExecutor taskExecutor;

    public SearchResultsTableDataModel(BibDatabaseContext bibDatabaseContext, PreferencesService preferencesService, StateManager stateManager, TaskExecutor taskExecutor) {
        NameDisplayPreferences nameDisplayPreferences = preferencesService.getNameDisplayPreferences();
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.fieldValueFormatter = new SimpleObjectProperty<>(new MainTableFieldValueFormatter(nameDisplayPreferences, bibDatabaseContext));

        populateEntriesViewModel();
        stateManager.getOpenDatabases().addListener((ListChangeListener<BibDatabaseContext>) change -> populateEntriesViewModel());
        entriesFiltered = new CustomFilteredList<>(entriesViewModel, BibEntryTableViewModel::isVisible);

        // We need to wrap the list since otherwise sorting in the table does not work
        entriesSorted = new SortedList<>(entriesFiltered);

        EasyBind.listen(stateManager.activeSearchQuery(SearchType.GLOBAL_SEARCH), (observable, oldValue, newValue) -> updateSearchMatches(newValue));
        stateManager.searchResultSize(SearchType.GLOBAL_SEARCH).bind(Bindings.size(entriesFiltered));
    }

    private void populateEntriesViewModel() {
        entriesViewModel.clear();
        for (BibDatabaseContext context : stateManager.getOpenDatabases()) {
            ObservableList<BibEntry> entriesForDb = context.getDatabase().getEntries();
            ObservableList<BibEntryTableViewModel> viewModelForDb = EasyBind.mapBacked(entriesForDb, entry -> new BibEntryTableViewModel(entry, context, fieldValueFormatter), false);
            entriesViewModel.addAll(viewModelForDb);
        }
    }

    private void updateSearchMatches(Optional<SearchQuery> query) {
        BackgroundTask.wrap(() -> entriesViewModel.forEach(entry -> entry.isVisibleBySearch().set(isMatchedBySearch(query, entry))))
                      .onSuccess(result -> entriesFiltered.refilter())
                      .executeWith(taskExecutor);
    }

    private boolean isMatchedBySearch(Optional<SearchQuery> query, BibEntryTableViewModel entry) {
        return query.map(matcher -> matcher.isMatch(entry.getEntry())).orElse(true);
    }

    public SortedList<BibEntryTableViewModel> getEntriesFilteredAndSorted() {
        return entriesSorted;
    }
}
